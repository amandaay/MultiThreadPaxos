import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * The PaxosServerCreator class is responsible for creating and binding the Paxos servers
 * within the RMI registry. It also configures the acceptors and learners for each server.
 */
public class PaxosServerCreator {
  /**
   * The main method to launch the creation and binding process of the Paxos servers.
   *
   * @param args Command-line arguments (unused in this context).
   */
  public static void main(String[] args) {
    try {
      System.out.println("Starting the server...");
      boolean enableTimeout = true; // Default
      if (args.length != 3) {
        System.err.println("Usage: java PaxosServerCreator <IP address> <serverPort> <true/false to enable timeout>");
        System.exit(1);
      }
      if (args[2].equalsIgnoreCase("false")) {
        enableTimeout = false; // Set to false if "false" for timeout
      }
      int numServers = 5; // Total number of servers
      String host = args[0];
      int basePort = Integer.parseInt(args[1]); // Starting port number

      Server[] servers = new Server[numServers];

      // Create and bind servers
      for (int serverId = 0; serverId < numServers; serverId++) {
        int port = basePort + serverId; // Increment port for each server

        // Create RMI registry at the specified port
        Registry registry = LocateRegistry.createRegistry(port);

        // Create server instance
        servers[serverId] = new Server(serverId, numServers, enableTimeout);

        // Bind the server to the RMI registry
        registry.rebind("KVStoreInterface", servers[serverId]);

        System.out.println("Server " + serverId + " is ready at port " + port);

      }

      // Set acceptors and learners for each server
      for (int serverId = 0; serverId < numServers; serverId++) {
        AcceptorInterface[] acceptors = new AcceptorInterface[numServers];
        LearnerInterface[] learners = new LearnerInterface[numServers];
        for (int i = 0; i < numServers; i++) {
          if (i != serverId) {
            acceptors[i] = servers[i];
            learners[i] = servers[i];
          }
        }
        servers[serverId].setAcceptors(acceptors);
        servers[serverId].setLearners(learners);
      }
      System.out.println("Servers ready...");

      while (true) {
        try {
          Thread.sleep(1000); // Sleep for 1 second (adjust as needed)
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
      e.printStackTrace();
    }
  }
}
