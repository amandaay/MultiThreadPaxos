import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the Key-Value Store.
 */
public interface KVStoreInterface extends Remote {

  /**
   * Retrieves the value associated with the given key.
   *
   * @param key The key for which to retrieve the value.
   * @throws RemoteException if a remote communication error occurs.
   */
  void get(String key) throws RemoteException;

  /**
   * Stores the given key-value pair.
   *
   * @param key   The key to store.
   * @param value The value to associate with the key.
   * @throws RemoteException if a remote communication error occurs.
   */
  void put(String key, String value) throws RemoteException;

  /**
   * Deletes the entry with the given key.
   *
   * @param key The key to delete.
   * @throws RemoteException if a remote communication error occurs.
   */
  void delete(String key) throws RemoteException;

  /**
   * Receives a client response on the server.
   *
   * @param response The response received from the client.
   * @throws RemoteException if a remote communication error occurs.
   */
  void receiveClientResponse(String response) throws RemoteException;
}
