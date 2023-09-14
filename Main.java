import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Main {
  // São criados 3 HashMaps que guardam os valores nas memórias
  private static HashMap<Integer, String> virtualMemory = new HashMap<Integer, String>(4);
  private static HashMap<Integer, String> physicalMemory = new HashMap<Integer, String>(2);
  private static HashMap<Integer, String> diskMemory = new HashMap<Integer, String>(2);

  // É criado um lock para conduzir de melhor forma o acesso aos dados (escrita e
  // leitura) de
  // forma que apenas uma thread acesse por vez
  private static Lock lock = new ReentrantLock();

  public static void main(String[] args) throws InterruptedException {
    // Input dos dados que é mandado para as thread
    ArrayList<String> input_1 = new ArrayList<>(Arrays.asList("3-W-8 4-W-TESTE 2-W-123".split(" ")));
    ArrayList<String> input_2 = new ArrayList<>(Arrays.asList("2-W-DADO2 5-W-ASD".split(" ")));

    // São criados 2 runnables de execução
    Runnable runnable1 = new ExecRunnable(input_1);
    Runnable runnable2 = new ExecRunnable(input_2);

    // 2 threads são criadas para executar os runnables
    Thread thread1 = new Thread(runnable1);
    Thread thread2 = new Thread(runnable2);

    // As threas são iniciadas
    thread1.start();
    thread2.start();
  }

  // Runnable responsável pela inserção de dados
  public static class InsertRunnable implements Runnable {
    private String value;
    private int virtualKey;
    private String command;

    // Recebe o comando, a chave virtual e o valor a ser inserido
    public InsertRunnable(String command, int virtualKey, String value) {
      this.value = value;
      this.virtualKey = virtualKey;
      this.command = command;
    }

    @Override
    public void run() {
      lock.lock();
      System.out.println("----------------------------------------");
      System.out.println("# Comando da vez: " + command);

      // Checa se a chave já existe na memoria virtual
      if (virtualMemory.get(virtualKey) != null) {
        System.out.println("----------------------------------------");
        System.out.println("Chave existente na memória virtual - chave: " + virtualKey);
        System.out.println("Alterando valor conforme instrucao");
        System.out.println("----------------------------------------");

        String memoryType = virtualMemory.get(virtualKey).split(" - ")[0];
        Integer memoryIndex = Integer.parseInt(virtualMemory.get(virtualKey).split(" - ")[1]);

        // Checa e altera comforme o tipo de memória encontrada
        if (memoryType.equals("physical")) {
          physicalMemory.put(memoryIndex, this.value);

          System.out.println("----------------------------------------");
          System.out.println("Valor em memória fisica alterado para: [" + this.value + "]");
          System.out.println("----------------------------------------");
        } else if (memoryType.equals("disk")) {
          diskMemory.put(memoryIndex, this.value);

          System.out.println("----------------------------------------");
          System.out.println("Valor em memória em disco alterado para: [" + this.value + "]");
          System.out.println("----------------------------------------");
        }
      } else {
        // Checa se há espaco na memória física
        if (physicalMemory.size() < 2) {
          virtualMemory.put(virtualKey, "physical - " + physicalMemory.size());
          physicalMemory.put(physicalMemory.size(), this.value);

          System.out.println("----------------------------------------");
          System.out.println("Memoria virtual - Inserindo referência na chave: " + virtualKey);
          System.out
              .println(
                  "Inserido valor na Memória física - [" + this.value + "] - chave " + (physicalMemory.size() - 1));
          System.out.println("----------------------------------------");

          // Checa se há espa;o na memória em disco
        } else if (diskMemory.size() < 2) {
          Integer primeiraChave = null;
          Integer ultimaChave = null;

          System.out.println("----------------------------------------");
          System.out.println("Memória física cheia - iniciando swap por FIFO");
          System.out.println("----------------------------------------");

          // Encontra a primeira chave da memória física
          for (Entry<Integer, String> entry : physicalMemory.entrySet()) {
            primeiraChave = entry.getKey();
            break;

          }

          // Encontra a ultima chave da memória física
          for (Entry<Integer, String> entry : physicalMemory.entrySet()) {
            ultimaChave = entry.getKey();
          }

          if (primeiraChave != null) {
            Integer posicaoMemoriaVirtual = null;

            // encontra a posicão na memória virtual
            for (Entry<Integer, String> entry : virtualMemory.entrySet()) {
              if (entry.getValue().equals("physical - " + primeiraChave)) {
                posicaoMemoriaVirtual = entry.getKey();
                break;
              }
            }

            // Move a primeira posicão da memória física para a memória em disco
            virtualMemory.put(posicaoMemoriaVirtual, ("disk - " + diskMemory.size()));
            diskMemory.put(diskMemory.size(), physicalMemory.get(primeiraChave));
            physicalMemory.remove(primeiraChave);

            System.out.println("----------------------------------------");
            System.out.println("Primeira posicão: " + primeiraChave + " movida para memória em disco.");
            System.out.println("Enderecamento na memória virtual alterado");
            System.out.println("----------------------------------------");

            // Acrescentar o novo valor na memória física
            virtualMemory.put(virtualKey, "physical - " + (ultimaChave + 1));
            physicalMemory.put(ultimaChave + 1, this.value);

            System.out.println("----------------------------------------");
            System.out.println("Memoria virtual - Inserindo referência na chave: " + virtualKey);
            System.out
                .println(
                    "Inserido valor na Memória física - [" + this.value + "] - chave " + (ultimaChave + 1));
            System.out.println("----------------------------------------");
          }

        } else {
          System.out.println("Não há mais espaço na memória!");
        }
      }
      // Libera o uso dos dados
      System.out.println("HASHMAP MEMORIA VIRTUAL: " + virtualMemory);
      System.out.println("HASHMAP MEMORIA FISICA: " + physicalMemory);
      System.out.println("HASHMAP MEMORIA EM DISCO: " + diskMemory);
      lock.unlock();
    }
  }

  // Runnable responsável pela leitura de dados
  public static class ReadRunnable implements Runnable {
    private String command;
    private int virtualKey;

    // Salva o tipo de memória que armazena o dado
    private String typeMemory;

    // Salva a chave de acesso ao dado salvo na memória física ou em disco
    private int memoryKey;

    // Recebe o comando e a chave virtual
    public ReadRunnable(String command, int virtualKEy) {
      this.virtualKey = virtualKEy;
      this.command = command;
    }

    @Override
    public void run() {
      // Garante que apenas uma Thread trabalhe nesses dados
      lock.lock();
      System.out.println("# Comando da vez: " + command);

      // Testa se há algum dado em memória
      if (virtualMemory.size() == 0) {
        System.out.println("Não há informações em memória!");
      } else {
        // Testa se a chave virtual existe
        if (virtualMemory.containsKey(virtualKey)) {
          // Busca o tipo de memória (física ou virtual)
          this.typeMemory = virtualMemory.get(virtualKey).split(" - ")[0];

          // Busca a chave de acesso a memória
          this.memoryKey = Integer.parseInt(virtualMemory.get(virtualKey).split(" - ")[1]);

          System.out.println("----------------------------------------");
          System.out.println("Lendo Memória Virtual - " + virtualMemory.get(virtualKey));

          // Checa o tipo de memória e exibe o valor do dado
          if (typeMemory.equals("physical")) {
            System.out.println("Lendo Memória Física - Value: [" + physicalMemory.get(memoryKey) + "]");
          } else {
            System.out.println("Lendo Memória Disco - Value: [" + diskMemory.get(memoryKey) + "]");
          }
          System.out.println("----------------------------------------");
        } else {
          throw new IllegalArgumentException("Endereço inválido: " + virtualKey);
        }
      }
      // Libera o uso dos dados
      lock.unlock();
    }
  }

  // Runnable responsável por classificar e iniciar cada comando
  public static class ExecRunnable implements Runnable {
    private ArrayList<String> commandsList;

    // Recebe um ArrayList com todos os comandos da thread
    public ExecRunnable(ArrayList<String> commandsList) {
      this.commandsList = commandsList;
    }

    @Override
    public void run() {
      // Itera sobre cada comando
      for (String command : commandsList) {
        // Separa as estruções recebidas
        ArrayList<String> instructions = new ArrayList<>(Arrays.asList(command.split("-")));

        // Recebe a chave de memória virtual
        int virtualKey = Integer.parseInt(instructions.get(0));

        // Checa qual o tipo de ação (escrita-W ou leitura-R)
        if (instructions.get(1).equals("W")) {
          // Pega o valor que será salvo na memória
          String value = instructions.get(2);

          // Inicia uma thread de escrita apartir das estruções
          new Thread(new InsertRunnable(command, virtualKey, value)).start();
        } else if (instructions.get(1).equals("R")) {
          // Inicia uma thread para leitura a partir das estruções
          new Thread(new ReadRunnable(command, virtualKey)).start();
        }
      }

      // Após o loop limpa a lista de comandos
      commandsList.clear();
    }
  }
}
