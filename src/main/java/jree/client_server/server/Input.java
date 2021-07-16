package jree.client_server.server;

import java.util.Scanner;

final class Input {
    private final static Scanner INPUT = new Scanner(System.in);
    public static int getInt(String msg) {
        System.out.print(msg+" : ");
        String read = INPUT.nextLine();
        return Integer.parseInt(read);
    }

    public static String getString(String msg) {
        System.out.print(msg+" : ");
        return INPUT.nextLine();
    }
}
