package sp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Norbert Potocki
 */
public class Helper {

    /* Sprawdż, czy dana linijka tekstu jest identyfikatorem skrótu */
    public static boolean isShortcut(String str) {

        // Zaczynamy z dużej litery i interesują nas pełne wyrazy
        if (!str.matches("[\\p{Lu}][\\p{L}]*")) {
            return false;
        }

        return true;
    }

    /* Sprawdż, czy w linijce opisującej wyraz djvu (msg) występuje słowo (search)
     * i wstaw skrót (snippet)
     */
    public static String createSnippet(String msg, String search, String snippet, String color) {

        // Czy słowo pasuje?
        if (msg.trim().startsWith("(word") && msg.contains(search)) {

            // Pobieramy współrzędne słowa na stronie
            // Format: (word xmin ymin xmax ymax "słowo")
            msg = msg.trim();

            int xmin = Integer.valueOf(msg.replaceAll("\\(word ([0-9]+) .*", "$1"));
            int ymin = Integer.valueOf(msg.replaceAll("\\(word [0-9]+ ([0-9]+) .*", "$1"));
            int xmax = Integer.valueOf(msg.replaceAll("\\(word [0-9]+ [0-9]+ ([0-9]+) .*", "$1"));
            int ymax = Integer.valueOf(msg.replaceAll("\\(word [0-9]+ [0-9]+ [0-9]+ ([0-9]+) .*", "$1"));

            int width = xmax - xmin;
            int height = ymax - ymin;

            if(color == null)
                color = "#90EE90";

            // Tworzymy znacznik skrótu
            return "(maparea \"\" \""
                    + snippet
                    + "\" (rect "
                    + xmin + " " + ymin + " " + width + " " + height
                    + ") (none) (hilite " + color + "))";
        }

        return null;
    }

    // Wykonaj komendę i zwróć jej output
    public static String readFromCommand(String[] commands) throws Exception {
        try {
            String c = new String();
            for (int i =0; i < commands.length; i++)
                c += commands[i] + ' ';
            System.out.println("Executing command: " + c);
            String result = new String();
            Process child = Runtime.getRuntime().exec(commands);
            InputStream in = child.getInputStream();

            // Wczytaj output
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            result = writer.toString();

            in.close();

            return result;

        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("Nie udało się wykonać polecenia djvused! Sprawdź, czy podałeś dobrą"
                    + " ścieżkę do plików słownika oraz czy masz zainstalowany program djvused.");
            throw e;
        }
    }

        // Wykonaj komendę i zwróć jej output
    public static InputStream inputStreamFromCommand(String[] commands) throws Exception {
        try {
            String c = new String();
            for (int i =0; i < commands.length; i++)
                c += commands[i] + ' ';
            //System.out.println("Executing command: " + c);
            Process child = Runtime.getRuntime().exec(commands);
            InputStream in = child.getInputStream();
            return in;

        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("Nie udało się wykonać polecenia djvused! Sprawdź, czy podałeś dobrą"
                    + " ścieżkę do plików słownika oraz czy masz zainstalowany program djvused.");
            throw e;
        }
    }

    // Wykonaj komendę przekazując message na jej input
    public static void writeToCommand(String[] commands, String message) throws Exception {
        try {
            String c = new String();
            for (int i =0; i < commands.length; i++)
                c += commands[i] + ' ';
            //System.out.println("Executing command: " + c);
            Process child = Runtime.getRuntime().exec(commands);
            OutputStream out = child.getOutputStream();
            out.write(message.getBytes());

            out.close();
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("Nie udało się wykonać polecenia djvused! Sprawdź, czy podałeś dobrą"
                    + " ścieżkę do plików słownika oraz czy masz zainstalowany program djvused.");
            throw e;
        }
    }

    // Przekoduj napis na format wyświetlany przez djvused
    public static String utf8toOctal(String str) throws Exception {
        try {
            String res = "";
            for (int i = 0; i < str.length(); i++) {
                byte[] r = str.substring(i, i+1).getBytes("UTF-8");

                if(r.length == 1) {
                    res += str.substring(i, i+1);
                    continue;
                }

                for (int k = 0; k < r.length; k++) {
                    int tmp = r[k];
                    if (tmp < 0) {
                        tmp += 256;
                    }
                    //System.out.println(Integer.toOctalString(tmp));
                    res += "\\" + Integer.toOctalString(tmp);
                }
            }
            return res;

        } catch (UnsupportedEncodingException e) {
            System.err.println("Błąd przy kodowaniu znaków!");
            throw e;
        }
    }
}
