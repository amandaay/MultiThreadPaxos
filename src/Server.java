import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of a Server class that represents a node in a Paxos distributed consensus system.
 * This server plays the role of Proposer, Acceptor, and Learner in the Paxos algorithm, and it also handles key-value store operations.
 */
public class Server implements ProposerInterface, AcceptorInterface, LearnerInterface, KVStoreInterface, Serializable {
  private ConcurrentHashMap<String, String> kvStore = new ConcurrentHashMap<>();
  private AcceptorInterface[] acceptors;
  private LearnerInterface[] learners;
  private int numServers;
  private int serverId;
  private int highestPromisedProposalId = -1;
  private int highestAcceptedProposalId = -1;
  private Operation acceptedProposalValue = null;
  private AtomicInteger proposalNumber = new AtomicInteger(0);
  private String response = "";
  private static final long THREAD_TIMEOUT = 5000L; // 5 seconds
  private boolean enableTimeout = true;


  /**
   * Constructor to create a Server instance.
   * @param serverId The unique ID of this server.
   * @param numServers The total number of servers in the system.
   */
  public Server(int serverId, int numServers, boolean enableTimeout) {
    this.numServers = numServers;
    this.serverId = serverId;
    this.enableTimeout = enableTimeout;
  }

  /**
   * Generates a unique proposal ID.
   * @return A unique proposal ID.
   */
  private synchronized int generateProposalId() {
    // generate a unique proposal ID
    int currentProposalNumber = proposalNumber.getAndIncrement();
    return (serverId * numServers) + currentProposalNumber;
  }

  /**
   * Set the acceptors for this server.
   * @param acceptors Array of acceptors.
   */
  public void setAcceptors(AcceptorInterface[] acceptors) {
    this.acceptors = acceptors;
  }

  /**
   * Set the learners for this server.
   * @param learners Array of learners.
   */
  public void setLearners(LearnerInterface[] learners) {
    this.learners = learners;
  }

  @Override
  public synchronized void get(String key) throws RemoteException {
    proposeOperation(new Operation("GET", key));
  }

  @Override
  public synchronized void put(String key, String value) throws RemoteException {
    proposeOperation(new Operation("PUT", key, value));
  }

  @Override
  public synchronized void delete(String key) throws RemoteException {
    proposeOperation(new Operation("DELETE", key, null));
  }

  @Override
  public void receiveClientResponse(String response) throws RemoteException {
    this.response = response;
  }

  public String setResponse() {
    return this.response;
  }

  /**
   * Propose an operation to be applied.
   * @param operation The operation to be proposed.
   * @throws RemoteException If a remote error occurs.
   */
  private synchronized void proposeOperation(Operation operation) throws RemoteException {
    int proposalId = generateProposalId();
    propose(proposalId, operation);
  }

  @Override
  public synchronized int prepare(int proposalId) throws RemoteException {
    // Implement Paxos prepare logic here
    if (proposalId >= this.highestPromisedProposalId) {
      this.highestPromisedProposalId = proposalId;
      this.highestAcceptedProposalId = proposalId;
      return this.highestAcceptedProposalId;
    } else {
      return -1;
    }
  }

  @Override
  public synchronized boolean accept(int proposalId, Object proposalValue) throws RemoteException {
    // Simulate acceptor failure randomly
    if (shouldSimulateAcceptorFailure()) {
      System.out.println("Simulating acceptor failure...");
      return false;
    }

    // Actual Paxos logic
    if (proposalId >= highestPromisedProposalId) {
      highestPromisedProposalId = proposalId;
      highestAcceptedProposalId = proposalId;
      acceptedProposalValue = (Operation) proposalValue;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Helper method to decide whether to simulate acceptor failure during the accept phase.
   * @return True if failure should be simulated, false otherwise.
   */
  private boolean shouldSimulateAcceptorFailure() {
    if (!enableTimeout) {
      // If timeout mechanism is disabled, never simulate failure
      return false;
    }
    // Simulate acceptor failure with 50% probability
    return new Random().nextDouble() < 0.5;
  }

  /**
   * Simulate an acceptor failure and restart after a delay.
   */
  private void simulateAcceptorFailure() {
    if (!enableTimeout) {
      return; // Do nothing if timeout mechanism is disabled
    }
    System.out.println("Acceptor thread interrupted. Simulating failure...");
    try {
      Thread.currentThread().interrupt();
      // Sleep for a random duration (between 2 to 7 seconds)
      Thread.sleep(new Random().nextInt(5000) + 2000);
    } catch (InterruptedException e) {
      // Handle interruption (cleanup, log, etc.)
      System.out.println("Acceptor thread interrupted during simulation. Cleaning up...");
      Thread.currentThread().interrupt();
      System.out.println("Restarting acceptor thread...");
      initializeAndStartAcceptorThread();
    }
  }

  private void initializeAndStartAcceptorThread() {
    Thread acceptorThread = new Thread(() -> {
      try {
        while (true) {
          // Simulate acceptor failure with random timeouts
          Thread.sleep(new Random().nextInt((int) THREAD_TIMEOUT));
          System.out.println("Acceptor thread failed on server " + this.serverId);
          // Simulate restarting acceptor thread
          simulateAcceptorFailure();
        }
      } catch (InterruptedException e) {
        // Handle interruption (cleanup, log, etc.)
        System.out.println("Acceptor thread interrupted. Cleaning up...");
      }
    });
    acceptorThread.start();
  }


  @Override
  public synchronized void propose(int proposalId, Object proposalValue) throws RemoteException {
    System.out.println(Utils.getCurrentTimestamp() + ", Server " + serverId + " proposing operation: " + ((Operation) proposalValue).toString());
    // Implement Paxos propose logic here
    int prepareCount = 0;
    for (int i = 0; i < numServers; i++) {
      if (acceptors[i] != null) {
        int responseId = acceptors[i].prepare(proposalId);
        if (responseId == proposalId) {
          prepareCount++;
        }
      }
      if (prepareCount >= (numServers / 2) + 1) {
        break;
      }
    }

    if (prepareCount >= (numServers / 2) + 1) {
      for (int i = 0; i < numServers; i++) {
        if (acceptors[i] != null) {
          acceptors[i].accept(proposalId, proposalValue);
        }
      }
      for (int i = 0; i < numServers; i++) {
        if (learners[i] != null) {
          learners[i].learn(proposalId, proposalValue);
        }
      }
    }
  }

  @Override
  public synchronized void learn(int proposalId, Object acceptedValue) throws RemoteException {
    // Implement Paxos learn logic here
    if (proposalId >= highestAcceptedProposalId) {
      highestAcceptedProposalId = proposalId;
      acceptedProposalValue = (Operation) acceptedValue;
      applyOperation(acceptedProposalValue);
    }
  }

  /**
   * Apply the given operation to the key-value store.
   * @param operation The operation to apply.
   */
  private synchronized void applyOperation(Operation operation) {
    if (operation == null) return;
    switch (operation.type) {
      case "GET":
        if (!kvStore.containsKey(operation.key)) {
          this.response = "Key does not exist, try another key.";
        } else {
          this.response = "Here is your value " + kvStore.get(operation.key);
        }
        break;
      case "PUT":
        if (kvStore.containsKey(operation.key)) {
          this.response = "Key already exist, try another key.";
        } else {
          kvStore.put(operation.key, operation.value);
          this.response = "OK saved operation: {key= " + operation.key + ", value= " + operation.value + "}\n" + Utils.getCurrentTimestamp() + ", current operations in server" + this.serverId + ": " + kvStore;
        }
        break;
      case "DELETE":
        if (!kvStore.containsKey(operation.key)) {
          this.response = "Key does not exist.\noperations left: " + kvStore;
        } else {
          kvStore.remove(operation.key);
          this.response = "Deleted key as requested.\nOperations left: " + kvStore;
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation type: " + operation.type);
    }
    System.out.println(Utils.getCurrentTimestamp() + ", " + this.response);
  }

  public String printReceivedOperation(Operation operation) {
    return Utils.getCurrentTimestamp() + ", Received operation in Server " + serverId + ": " + operation.toString();
  }

  /**
   * Static class representing an operation on the key-value store.
   */
  private static class Operation {
    String type;
    String key;
    String value;

    Operation(String type, String key, String value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }

    Operation(String type, String key) {
      this(type, key, null);
    }

    @Override
    public String toString() {
      if (value == null) {
        return type + " " + key;
      } else {
        return type + " " + key + " " + value;
      }
    }
  }
}
