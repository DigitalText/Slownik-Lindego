package sp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**norbert
 *
 * @author Norbert Potocki
 */
public class Main {

    public static Map<String, String> shortcuts = new HashMap<String, String>();
    public static Map<String, Integer> statistics = new HashMap<String, Integer>();
    public static Boolean verbose = false;
    public static Boolean calculateStatistics = false;
    public static Boolean xml_included = false;

    public static void main(String[] args) {

        String shortcutsFileName = null;
        String dictionaryFileName = null;
        String xmlFileName = null;
        String color = null;

        /* Parsuj linię komend */
        int i = 0;
        String arg;

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            if (arg.equals("-h")) {
                i = 9999;
                break;
            } else if (arg.equals("-v")) {
                Main.verbose = true;
            } else if (arg.equals("-xml")) {
                if (i < args.length) {
                    xmlFileName = args[i++];
                    Main.xml_included = true;
                } else {
                    System.err.println("Uwaga: -xml wymaga podania nazwy pliku\n");
                    return;
                }
            } else if (arg.equals("-a")) {
                Main.calculateStatistics = true;
            } else if (arg.equals("-s")) {
                if (i < args.length) {
                    shortcutsFileName = args[i++];
                } else {
                    System.err.println("Uwaga: -s wymaga podania nazwy pliku\n");
                    return;
                }
            } else if (arg.equals("-d")) {
                if (i < args.length) {
                    dictionaryFileName = args[i++];
                    if (!dictionaryFileName.endsWith(".djvu")) {
                        System.err.println("Uwaga: plik wynikowy musi mieć format djvu\n");
                        return;
                    }
                } else {
                    System.err.println("Uwaga: -d wymaga podania nazwy pliku\n");
                    return;
                }
            } else if (arg.equals("-c")) {
                if (i < args.length) {
                    color = args[i++];
                } else {
                    System.err.println("Uwaga: -c wymaga podania koloru w formacie '#XXXXXX'\n");
                    return;
                }
            }
        }

        //System.out.println("" + i + " " + args.length);

        if (i != args.length || shortcutsFileName == null || dictionaryFileName == null) {
            System.err.println("Użycie: java -jar SP.jar -s plik -d plik [-v] [-c '#XXXXXX'] [-a]"
                    + "\n\nParametry:"
                    + "\n         -s nazwa_pliku    - nazwa pliku zawierającego skróty"
                    + "\n         -d nazwa_pliku    - nazwa pliku do konwersji (xml lub djvu)"
                    + "\n         -c '#XXXXXX'      - nazwa kolor tła dla skrótu w notacji szestnastkowej"
                    + "\n         -v                - pokazuj co robisz"
                    + "\n         -a                - pokaż statystyki uzycia skrótów"
                    + "\n         -xml nazwa pliku  - xml odpowiadajacy plikowi -d");
            return;
        } else {

            if (Main.verbose) {
                System.out.println("Rozpoczynam przetwarzanie:"
                        + "\n- plik ze skrótami: " + shortcutsFileName
                        + "\n- plik do translacji: " + dictionaryFileName + "\n");
            }
            // Przygotuj listę skrótów
            try {
                prepareShortcuts(shortcutsFileName);

                // Przetwórz pliki słownika
                processPages(dictionaryFileName, color, xmlFileName);
            } catch (Exception exception) {
                System.err.println("\nWystąpił błąd w działaniu programu!");

                System.err.println("\n\nOpis błędu:");
                exception.printStackTrace();
            }
        }
    }

    /* Funkcja przygotowuje listę skrótów na podstawie dostarczonego pliku */
    private static void prepareShortcuts(String fname) throws Exception {
        File file = new File(fname);
        FileInputStream fis = null;
        BufferedReader br = null;

        try {
            fis = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(fis));

            String key = null;
            String val = null;

            // Wczytaj plik linia po lini
            while (br.ready()) {
                String data = br.readLine();

                data = data.trim();

                // Sprawdź, czy dana linijka jest identyfikatorem skrótu
                if (Helper.isShortcut(data)) {

                    // Czy w buforze znajduje się już skrót?
                    if (key != null) {
                        // Jeśli tak, to zapisz go do zbioru skrótów
                        Main.shortcuts.put(Helper.utf8toOctal(key), Helper.utf8toOctal(val));

                        if (calculateStatistics) {
                            Main.statistics.put(Helper.utf8toOctal(key), 0);
                        }
                    }

                    // Utwórz nowy skrót
                    key = data;
                    val = new String();
                } else {
                    // Dana linijka jest opisem skrótu
                    val += data;
                }
            }

            fis.close();
            br.close();

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            System.err.println("Nie znaleziono pliku ze skrótami!");
            throw e;
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("Błąd podczas odczytu pliku ze skrótami!");
            throw e;
        }
    }

    // Przetwórz strony słownika z danego katalogu, dodając do nich skróty
    private static void processPages(String path, String color, String xml_path) throws Exception {

        try {
            File file = new File(path);

            if (Main.verbose) {
                System.err.println("Przetwarzam plik: " + file.getName());
            }

            // Pobierz tekstową strukturę pliku .djvu
            if (Main.verbose) {
                System.err.println("Wykonuję polecenie: djvused -e 'output-all' " + file.getCanonicalPath());
            }

            BufferedReader inputBuffer;
           
            if (Main.xml_included)
            {
                File xml_fle = new File(xml_path);
                FileInputStream fis = new FileInputStream(xml_fle.getCanonicalPath());
                DataInputStream in = new DataInputStream(fis);
                inputBuffer = new BufferedReader(new InputStreamReader(in));
            }
            else
            {
                String[] commands = new String[]{"djvused", "-e", "'output-all'", file.getCanonicalPath()};
                String inputScript = Helper.readFromCommand(commands);
                inputBuffer = new BufferedReader(new StringReader(inputScript));
            }
            StringWriter outputBuffer = new StringWriter();

            // Lista znaczników (format djvu) skrótów do dodania do aktualnie przetwarzanej strony
            Set<String> annots = null;

            // Czy dana strona posiada tekst
            boolean pageHasText = false;
            
            // Dodajemy skróty do stron, sprawdzając wyraz po wyrazie, czy trafiliśmy na skrót
            
            while (inputBuffer.ready()) {

                // Pobierz linijkę opisującą plik
                String line = inputBuffer.readLine();

                // Koniec pliku
                if (line == null) {
                    break;
                }

                // Koniec sekcji z tekstem na tej stronie
                if (pageHasText && line.equals(".")) {
                    // Zapisz utworzone anotacje

                    // Zapisujemy wczytaną właśnie kropkę
                    outputBuffer.append(".\n");

                    // Dodaj sekcję opisującą skróty
                    outputBuffer.append("set-ant\n");

                    // Dodaj znaczniki skrótów do sekcji
                    for (Iterator<String> it = annots.iterator(); it.hasNext();) {
                        String anno = it.next();
                        outputBuffer.append(anno + "\n");
                    }

                    // Zamknij sekcję
                    outputBuffer.append(".\n");

                    // Zakończ przetwarzanie i nie kontynuuj przetwarzania tej linijki
                    pageHasText = false;
                    continue;
                }

                // Nowa strona?
                if (line.trim().startsWith("select ")) {
                    annots = new HashSet<String>();
                }

                // Jesteśmy wewnątrz sekcji z tekstem
                if (pageHasText) {

                    // Sprawdź, czy trafimy jakiś skrót
                    Iterator it = shortcuts.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();

                        // Sprawdź, czy udało się coś dopasować i przygotuj znacznik skrótu
                        String wst = Helper.createSnippet(line, entry.getKey(), entry.getValue(), color);

                        // Jeśli udało się dopasować, zapisz nowy skrót do dodania
                        if (wst != null) {
                            annots.add(wst);

                            if (calculateStatistics) {
                                String key = entry.getKey();
                                Integer old = Main.statistics.remove(entry.getKey());
                                Main.statistics.put(key, old + 1);
                            }
                        }
                    }
                }

                if (line.trim().startsWith("set-txt")) {
                    pageHasText = true;
                }

                // Linijka zbadana, zapisz ją na wyjście
                outputBuffer.write(line + "\n");
            }

            // Całość wczytana
            inputBuffer.close();

            String outputScript = outputBuffer.toString();
            outputBuffer.close();

            // Złóż ponownie stronę .djvu na podstawie skryptów
            if (Main.verbose) {
                //System.err.println(outputScript);
                System.err.println("Wykonuję polecenie: djvused " + file.getCanonicalPath() + " -s");
            }

            String[] commands2 = new String[]{"djvused", file.getCanonicalPath(), "-s"};
            //System.out.print(outputScript);
            Helper.writeToCommand(commands2, outputScript);

            // Wypisz statystyki
            if (calculateStatistics) {
                System.err.println("\n------- Statystyki (liczba wystąpień - skrót) -------\n");

                Iterator it = statistics.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();

                    System.err.println(entry.getValue() + " - " + entry.getKey());
                }
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas zapisu strony!");
            throw e;
        }
    }
}
