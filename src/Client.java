import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
  private KVStoreInterface server;

  // Constructor to initialize the client by looking up the remote KVStoreInterface
  public Client(String host, int basePort) throws Exception {
    // Get the RMI registry on the specified host and port
    Registry registry = LocateRegistry.getRegistry(host, basePort);

    // Look up the remote object with the name "KVStoreInterface" in the registry
    server = (KVStoreInterface) registry.lookup("KVStoreInterface");
  }

  // Method to perform a GET operation and return the result
  public String get(String key) {
    try {
      // Invoke the remote GET method on the server
      server.get(key);
      return Utils.getCurrentTimestamp() + ", receiving GET " + key;
    } catch (RemoteException e) {
      // Handle RemoteException if the operation fails
      return "GET operation failed: " + e.getMessage();
    }
  }

  // Method to perform a PUT operation and return the result
  public String put(String key, String value) {
    try {
      // Construct the PUT operation string
      String operation = "PUT " + key + " " + value;

      // Invoke the remote PUT method on the server
      server.put(key, value);

      // Send the client response to the server
      sendClientResponseToServer(operation);

      // Return the result with the current timestamp
      return Utils.getCurrentTimestamp() + ", receiving " + operation;
    } catch (RemoteException e) {
      // Handle RemoteException if the operation fails
      return "PUT operation failed: " + e.getMessage();
    }
  }

  // Method to perform a DELETE operation and return the result
  public String delete(String key) {
    try {
      // Invoke the remote DELETE method on the server
      server.delete(key);
      return Utils.getCurrentTimestamp() + ", receiving DELETE " + key;
    } catch (RemoteException e) {
      // Handle RemoteException if the operation fails
      return "DELETE operation failed: " + e.getMessage();
    }
  }

  // Helper method to send a client response to the server
  private void sendClientResponseToServer(String operation) {
    try {
      // Invoke the remote method on the server to receive the client response
      server.receiveClientResponse(Utils.getCurrentTimestamp() + ", receiving " + operation);
    } catch (RemoteException e) {
      // Handle RemoteException if the operation fails
      System.err.println("Failed to send client response to the server: " + e.getMessage());
    }
  }

  // Main method to run the client application
  public static void main(String[] args) {
    System.out.println("Starting the Key Value Store Client, your port must be either <server port> or its increment of 1 to 4...\n");
    try {
      // Check if the correct number of command-line arguments is provided
      if (args.length != 2) {
        System.out.println("Usage: java KeyValueStoreClient <localhost> <port number: <server port> or its increment of 1 to 4>");
        System.exit(1);
      }

      // Parse the command-line arguments
      String localhost = args[0];
      int clientPort = Integer.parseInt(args[1]);

      // Look up the key-value store from the registry and establish connections
      Client client = new Client(localhost, clientPort);

      System.out.println("Enter at least 5 PUTs, 5 GETs, 5 DELETEs operation.\nHere is an example:");
      String response = "", timestamp = "";

      // Pre-populate the Key-Value store with data and a set of keys
      String[] prepopulate = {"a", "b", "c", "d", "e", "f"};
      for (int i = 0; i < 5; i++) {
        timestamp = Utils.getCurrentTimestamp();
        response = client.put(prepopulate[i], prepopulate[i + 1]);
        System.out.println(response);
        System.out.println(client.get(prepopulate[i]));
        System.out.println(client.delete(prepopulate[i]));
      }
      System.out.println(timestamp + ", Enter operation:\nPUT <key> <value> or GET <key> or DELETE <key>");

      // Perform at least 5 GETs, 5 PUTs, 5 DELETES
      while (true) {
        // Get the current timestamp
        timestamp = Utils.getCurrentTimestamp();
        String userInput = System.console().readLine();

        System.out.println(timestamp + ", Sending: " + userInput);

        // Split the user input
        String[] inputTokens = userInput.split("\\s+");
        if (inputTokens.length > 0) {
          String operation = inputTokens[0].toUpperCase();

          // Process client.get, put, delete...
          switch (operation) {
            case "GET" -> {
              if (inputTokens.length == 2) {
                response = client.get(inputTokens[1]);
              } else {
                System.out.println(timestamp + ", " + "Make sure there's one key to perform GET operation.");
              }
            }
            case "PUT" -> {
              if (inputTokens.length == 3) {
                response = client.put(inputTokens[1], inputTokens[2]);
              } else {
                System.out.println(timestamp + ", " + "Make sure there's one key value to perform PUT operation.");
              }
            }
            case "DELETE" -> {
              if (inputTokens.length == 2) {
                response = client.delete(inputTokens[1]);
              } else {
                System.out.println(timestamp + ", " + "Make sure there's one key to perform DELETE operation.");
              }
            }
            default -> System.out.println(timestamp + ", " + "Received an unknown operation. Try again. Or you didn't want to shut down.");
          }
        }
        System.out.println(timestamp + ", Enter operation:\nPUT <key> <value> or GET <key> or DELETE <key>");
      }
    } catch (Exception e) {
      // Handle exceptions that may occur during client execution
      System.err.println("Client exception: " + e.toString());
    }
  }
}
