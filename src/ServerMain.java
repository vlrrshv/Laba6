import Connection.DataToOutput;
import Connection.MessageForClient;
import collection.MyCollection;
import commands.*;
import data.FuelType;
import data.Vehicle;

import java.io.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class ServerMain {
    private static ServerSocketChannel serverSocketChannel;
    private static Selector selector;
    private static File file;

    public static void main(String[] args) throws IOException {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(3346));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            run();
        } catch (IOException e) {
            System.out.println("There is no such open server port");
        } finally {
            selector.close();
        }

    }

    private static void run() throws IOException {
        MyCollection collection = new MyCollection();
        Switcher switcher = new Switcher();
        MessageForClient message = new MessageForClient();
        while (true) {
            int count = selector.select();
            if (count == 0) {
                continue;
            }
            Set keySet = selector.selectedKeys();
            Iterator it = keySet.iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                it.remove();
                ByteBuffer buffer = ByteBuffer.allocate(65536);
                if (key.isAcceptable()) {
                    accept(key, buffer, collection);
                    break;
                }
                if (key.isReadable()) {
                    DataToOutput<?> command = read(key, buffer);
                    try {
                        String name = command.getCommandName();
                        System.out.println(name);
                        //System.out.println(collection.show());
                        switch (name) {
                            case "help":
                                AbstractCommand<String> help = new CommandHelp(collection);
                                switcher.setCommand(help);
                                message.setCommandIsDone(true);
                                message.setMessage(switcher.doCommand());
                                break;
                            case "info":
                                AbstractCommand<String> info = new CommandInfo(collection);
                                switcher.setCommand(info);
                                message.setCommandIsDone(true);
                                message.setMessage(switcher.doCommand());
                                break;
                            case "show":
                                AbstractCommand<String> show = new CommandShow(collection);
                                switcher.setCommand(show);
                                message.setCommandIsDone(true);
                                message.setMessage(switcher.doCommand());
                                break;
                            case "add":
                                AbstractCommand<Vehicle> add = new CommandAdd(collection);
                                add.setParameter((Vehicle) command.getArgument());
                                switcher.setCommand(add);
                                message.setCommandIsDone(true);
                                message.setMessage(switcher.doCommand());
                                break;
                            case "update":
                                AbstractCommand<Vehicle> updateById = new CommandUpdateByID(collection);
                                updateById.setParameter((Vehicle) command.getArgument());
                                switcher.setCommand(updateById);
                                String s = switcher.doCommand();
                                message.setCommandIsDone(s.equals("Element is updated"));
                                message.setMessage(s);
                                break;
                            case "remove_by_id":
                                AbstractCommand<Integer> removeById = new CommandRemoveById(collection);
                                removeById.setParameter((Integer) command.getArgument());
                                switcher.setCommand(removeById);
                                String removeId = switcher.doCommand();
                                message.setCommandIsDone(removeId.equals("Element was deleted"));
                                message.setMessage(removeId);
                                break;
                            case "clear":
                                AbstractCommand<String> clear = new CommandClear(collection);
                                switcher.setCommand(clear);
                                message.setCommandIsDone(!collection.checkToEmpty());
                                message.setMessage(switcher.doCommand());
                                break;
                            case "execute_script":
                                AbstractCommand<File> execute_script = new CommandExecuteScript(collection, file);
                                execute_script.setParameter((File) command.getArgument());
                                switcher.setCommand(execute_script);
                                String sc = switcher.doCommand();
                                message.setCommandIsDone(!sc.equals("This file doesn't exist"));
                                message.setMessage(sc);
                                break;
                            case "remove_first":
                                AbstractCommand<String> removeFirst = new CommandRemoveFirst(collection);
                                switcher.setCommand(removeFirst);
                                message.setCommandIsDone(!collection.checkToEmpty());
                                message.setMessage(switcher.doCommand());
                                break;
                            case "remove_head":
                                AbstractCommand<String> removeHead = new CommandRemoveHead(collection);
                                switcher.setCommand(removeHead);
                                message.setCommandIsDone(!collection.checkToEmpty());
                                message.setMessage(switcher.doCommand());
                                break;
                            case "add_if_max":
                                AbstractCommand<Vehicle> addIfMax = new CommandAddIFMax(collection);
                                addIfMax.setParameter((Vehicle) command.getArgument());
                                switcher.setCommand(addIfMax);
                                String addMax = switcher.doCommand();
                                message.setCommandIsDone(addMax.equals("Element was added"));
                                message.setMessage(addMax);
                                break;
                            case "remove_any_by_fuel_type":
                                AbstractCommand<FuelType> removeAnyBuFuelType = new CommandRemoveByAnyFuelType(collection);
                                removeAnyBuFuelType.setParameter((FuelType) command.getArgument());
                                switcher.setCommand(removeAnyBuFuelType);
                                String removeFuelType = switcher.doCommand();
                                message.setCommandIsDone(!removeFuelType.equals("Collection is empty. Add element firstly."));
                                message.setMessage(removeFuelType);
                                break;
                            case "max_by_name":
                                AbstractCommand<String> maxByName = new CommandMaxByName(collection);
                                switcher.setCommand(maxByName);
                                message.setCommandIsDone(collection.checkToEmpty());
                                message.setMessage(switcher.doCommand());
                                break;
                            case "group_counting_by_creation_date":
                                AbstractCommand<String> groupCount = new CommandGroupCounting(collection);
                                switcher.setCommand(groupCount);
                                message.setCommandIsDone(!collection.checkToEmpty());
                                message.setMessage(switcher.doCommand());
                                break;
                            case "fillFromFile":
                                AbstractCommand<List<Vehicle>> fillFromFile = new CommandFillFromFile(collection);
                                fillFromFile.setParameter((List<Vehicle>) command.getArgument());
                                switcher.setCommand(fillFromFile);
                                switcher.doCommand();
                                message.setCommandIsDone(true);
                                message.setMessage("lol");
                                break;
                            case "exit":
                                AbstractCommand<File> save = new CommandSave(collection);
                                save.setParameter(file);
                                switcher.setCommand(save);
                                String saved = switcher.doCommand();
                                message.setCommandIsDone(!saved.equals("File doesn't exist. Enter a command"));
                                message.setMessage(saved);
                                break;
                        }
                        break;
                    } catch (NullPointerException e) {
                        System.out.println("Client went out");
                        break;
                    }
                }
                if (key.isWritable()) {
                    write(key, message, buffer);
                    break;
                }
            }
        }

    }

    private static void accept(SelectionKey key, ByteBuffer buffer, MyCollection collection) {
        SocketChannel client;
        try {
            client = serverSocketChannel.accept();
            client.read(buffer);
            client.configureBlocking(false);
            client.register(key.selector(), SelectionKey.OP_READ);
            file = deserialize(buffer);
            System.out.println("Client connected");
            ServerInput input = new ServerInput(collection, file);
            input.start();
            buffer.clear();
        } catch (IOException | ClassNotFoundException ignored) {
            ignored.printStackTrace();
        }
    }

    private static void write(SelectionKey key, MessageForClient message, ByteBuffer buffer) {
        SocketChannel channel = (SocketChannel) key.channel();
        buffer.put(serialize(message));
        buffer.flip();
        try {
            channel.write(buffer);
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_READ);
            buffer.clear();
        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            //System.out.println("Client go out");
        }
    }

    private static byte[] serialize(MessageForClient message) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(message);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            System.out.println("Serialize problem");
        }
        return null;
    }

    private static DataToOutput<?> read(SelectionKey key, ByteBuffer buffer) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            channel.read(buffer);
            DataToOutput<?> command = deserialize(buffer);
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_WRITE);
            buffer.clear();
            return command;
        } catch (IOException | ClassNotFoundException e) {
            try {
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            //System.out.println("Client go out");
        }
        return null;
    }

    private static <T> T deserialize(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        T data;
        try {
            data = (T) objectInputStream.readObject();
        } catch (IOException e) {
            data = null;
        }
        byteArrayInputStream.close();
        objectInputStream.close();
        byteBuffer.clear();
        return data;
    }

}

