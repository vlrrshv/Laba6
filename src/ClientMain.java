import Connection.*;
import data.FuelType;
import data.Vehicle;


import reader.MyReader;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;


import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @author Ershova Valeria R3140
 * This is main class
 */
public class ClientMain {
    private static final Scanner scanner = new Scanner(System.in);
    private static SocketChannel socket;



    /**
     * This is start point of program
     *
     * @param args [0] - name of input file
     */
    public static void main(String[] args) {

        String fileName;
        List<Vehicle> list;
        while (true) {
            try {
                fileName = args[0];
                list = MyReader.readVehiclesFromFile(fileName);
                break;
            } catch (FileNotFoundException e) {
                System.out.println("This file doesn't exist. Enter file from console");
            } catch (IOException e) {
                System.out.println("Something went wrong with reading data from file. Enter file from console");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("You didn't enter a input file");
            }
            System.out.println("Enter a file's name for starting");
            fileName = scanner.nextLine();
            try {
                list = MyReader.readVehiclesFromFile(fileName);
                break;
            } catch (IOException e) {
                System.out.println("Something went wrong with reading file");
            }
        }
        try {

            socket = SocketChannel.open();
            socket.connect(new InetSocketAddress("localhost", 3346));
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            //DataToOutput<File> giveFile = new DataToOutput<>("giveFile", new File(fileName));
            buffer.put(serialize(new File(fileName)));
            buffer.flip();
            socket.write(buffer);
            buffer.clear();

            DataToOutput<List<Vehicle>> fillFromFile = new DataToOutput<>("fillFromFile", list);
            System.out.println(send(fillFromFile).getMessage());


            System.out.println("Enter a command");
            String command = scanner.nextLine();
            String[] commands = checkTask(command);

            while (true) {
                MessageForClient message = new MessageForClient();
                switch (commands[0]) {
                    case "help":
                        DataToOutput<String> help = new DataToOutput<>("help", "nothing");
                        message = send(help);
                        break;
                    case "info":
                        DataToOutput<String> info = new DataToOutput<>("info", "nothing");
                        message = send(info);
                        break;
                    case "show":
                        DataToOutput<String> show = new DataToOutput<>("show", "nothing");
                        message = send(show);
                        break;
                    case "add":
                        Vehicle vehicle = MyReader.getElementFromConsole(scanner);
                        DataToOutput<Vehicle> add = new DataToOutput<>("add", vehicle);
                        message = send(add);
                        break;
                    case "update":
                        int id = MyReader.getId(scanner, commands);
                        Vehicle vehicleToUpdate = MyReader.getElementFromConsoleToUpdate(scanner, id);
                        DataToOutput<Vehicle> update = new DataToOutput<>("update", vehicleToUpdate);
                        message = send(update);
                        break;
                    case "remove_by_id":
                        int idToRemove = MyReader.getId(scanner, commands);
                        DataToOutput<Integer> removeById = new DataToOutput<>("remove_by_id", idToRemove);
                        message = send(removeById);
                        break;
                    case "clear":
                        DataToOutput<String> clear = new DataToOutput<>("clear", "nothing");
                        message = send(clear);
                        break;
                    case "execute_script":
                        File scriptName = MyReader.getFileName(scanner, commands);
                        DataToOutput<File> executeScript = new DataToOutput<>("execute_script", scriptName);
                        message = send(executeScript);
                        if (message.getMessage().contains("Data is saved")){
                            commands[0] = "exit";
                        }
                        break;
                    case "remove_first":
                        DataToOutput<String> removeFirst = new DataToOutput<>("remove_first", "nothing");
                        message = send(removeFirst);
                        break;
                    case "remove_head":
                        DataToOutput<String> removeHead = new DataToOutput<>("remove_head", "nothing");
                        message = send(removeHead);
                        break;
                    case "add_if_max":
                        Vehicle vehicleAddIfMax = MyReader.getElementFromConsole(scanner);
                        DataToOutput<Vehicle> addIfMax = new DataToOutput<>("add_if_max", vehicleAddIfMax);
                        message = send(addIfMax);
                        break;
                    case "remove_any_by_fuel_type":
                        FuelType fuelType = MyReader.getFuelType(scanner, commands);
                        DataToOutput<FuelType> removeAnyFuelType = new DataToOutput<>("remove_any_by_fuel_type", fuelType);
                        message = send(removeAnyFuelType);
                        break;
                    case "max_by_name":
                        DataToOutput<String> maxByName = new DataToOutput<>("max_by_name", "nothing");
                        message = send(maxByName);
                        break;
                    case "group_counting_by_creation_date":
                        DataToOutput<String> groupByCreationDate = new DataToOutput<>("group_counting_by_creation_date", "nothing");
                        message = send(groupByCreationDate);
                        break;
                    case "exit":
                        DataToOutput<String> exit = new DataToOutput<>("exit", "nothing");
                        message = send(exit);
                        break;
                    default:
                        System.out.println("There is no this command. Try again");
                        break;
                }
                if (message != null) {
                    if (message.getMessage()!=null){
                        System.out.println("Command was done " + message.isCommandDone());
                        System.out.println(message.getMessage());
                        if (commands[0].equals("exit")){
                            System.exit(0);
                        }
                    }
                }else throw new ConnectException();
                System.out.println("Enter a command");
                command = scanner.nextLine();
                commands = checkTask(command);
            }
        } catch (ConnectException e){
            System.out.println("Server isn't working now. Try to connect later");
        } catch (IOException e) {
            System.out.println("There is no such port or port isn't available");
            //e.printStackTrace();
        } catch (NoSuchElementException e){
            MessageForClient messageCtrlD;
            DataToOutput<String> exit = new DataToOutput<>("exit", "nothing");
            messageCtrlD = send(exit);
            System.out.println("It was exit combination");
            System.out.println("Command was done " + messageCtrlD.isCommandDone());
            System.out.println(messageCtrlD.getMessage());
            System.exit(0);
        }
    }

    private static MessageForClient send(DataToOutput<?> command) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(65336);
            buffer.put(serialize(command));
            buffer.flip();
            socket.write(buffer);
            buffer.clear();
            socket.read(buffer);
            MessageForClient message = deserialize(buffer.array());
            buffer.clear();
            return message;
        } catch (IOException e) {
            //System.out.println("Server is closed");
            return null;
        }
    }

    private static MessageForClient deserialize(byte[] array) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (MessageForClient) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Deserialize error");
        }
        return null;
    }

    private static <T> byte[] serialize(T command) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(command);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            System.out.println("Serialize problem");
        }
        return null;
    }


    private static String[] checkTask(String task) {
        String[] commands = task.split(" ");
        String[] parameters = new String[2];
        parameters[0] = "";
        parameters[1] = "";
        try {
            for (int i = 0; i < commands.length; i++) {
                if ((commands[i].equals("help")) || (commands[i].equals("info")) || (commands[i].equals("show")) || (commands[i].equals("add")) ||
                        (commands[i].equals("update")) || (commands[i].equals("remove_by_id")) || (commands[i].equals("clear")) ||
                        (commands[i].equals("save")) || (commands[i].equals("execute_script")) || (commands[i].equals("remove_first")) ||
                        (commands[i].equals("remove_head")) || (commands[i].equals("add_if_max")) || (commands[i].equals("remove_any_by_fuel_type")) ||
                        (commands[i].equals("max_by_name")) || (commands[i].equals("group_counting_by_creation_date")) || (commands[i].equals("exit"))) {
                    parameters[0] = commands[i];
                    parameters[1] = commands[i + 1];
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            parameters[1] = "";
        }
        return parameters;
    }
}
