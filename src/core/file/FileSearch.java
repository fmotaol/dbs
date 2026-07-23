package core.file;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileSearch {
    
    /**
     * Busca arquivos usando wildcards como no CMD
     * Exemplo: "c:\telem\*.geojson"
     */
    public static File[] searchFiles(String pathWithWildcard) {
        if (pathWithWildcard == null || pathWithWildcard.trim().isEmpty()) {
            return new File[0];
        }
        
        // Separa o diretório do padrão do arquivo
        String[] parts = splitPathAndPattern(pathWithWildcard);
        String directoryPath = parts[0];
        String filePattern = parts[1];
        
        File directory = new File(directoryPath);
        
        // Verifica se o diretório existe
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("diretório não encontrado: " + directoryPath);
            return new File[0];
        }
        
        // Converte o padrão com wildcard para regex
        String regexPattern = convertWildcardToRegex(filePattern);
        
        // Filtra os arquivos
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Pattern.matches(regexPattern, name);
            }
        });
        
        return files != null ? files : new File[0];
    }
    
    /**
     * Separa o caminho do diretório do padrão do arquivo
     */
    private static String[] splitPathAndPattern(String pathWithWildcard) {
        int lastSeparator = pathWithWildcard.lastIndexOf(File.separator);
        if (lastSeparator == -1) {
            lastSeparator = pathWithWildcard.lastIndexOf("/");
        }
        
        if (lastSeparator == -1) {
            // não há separador de diretório, busca no diretório atual
            return new String[]{ ".", pathWithWildcard };
        }
        
        String directory = pathWithWildcard.substring(0, lastSeparator);
        String pattern = pathWithWildcard.substring(lastSeparator + 1);
        
        return new String[]{ directory, pattern };
    }
    
    /**
     * Converte padrão com wildcard para regex
     * * ? .*
     * ? ? .
     */
    private static String convertWildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '[':
                case ']':
                case '(':
                case ')':
                case '\\':
                case '^':
                case '$':
                case '|':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        
        return regex.toString();
    }
    
    /**
     * Busca recursivamente em subdiretórios
     */
    public static List<File> searchFilesRecursive(String pathWithWildcard) {
        List<File> result = new ArrayList<>();
        String[] parts = splitPathAndPattern(pathWithWildcard);
        String directoryPath = parts[0];
        String filePattern = parts[1];
        
        searchRecursive(new File(directoryPath), filePattern, result);
        return result;
    }
    
    private static void searchRecursive(File directory, String pattern, List<File> results) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        String regexPattern = convertWildcardToRegex(pattern);
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Busca recursivamente nas subpastas
                    searchRecursive(file, pattern, results);
                } else if (Pattern.matches(regexPattern, file.getName())) {
                    results.add(file);
                }
            }
        }
    }
    
    /**
     * Método main com exemplo de uso
     */
//    public static void main(String[] args) {
//        // Exemplo de uso
//        String path = "c:\\telem\\*.geojson";
//        
//        System.out.println("Busca simples:");
//        File[] files = searchFiles(path);
//        for (File file : files) {
//            System.out.println("Encontrado: " + file.getAbsolutePath());
//        }
//        
//        System.out.println("\nBusca recursiva:");
//        List<File> recursiveFiles = searchFilesRecursive(path);
//        for (File file : recursiveFiles) {
//            System.out.println("Encontrado: " + file.getAbsolutePath());
//        }
//        
//        // Outros exemplos
//        System.out.println("\nOutros exemplos:");
//        searchFiles("c:\\telem\\dados*.geojson"); // arquivos que começam com "dados"
//        searchFiles("c:\\telem\\mapa?.geojson");  // arquivos como "mapa1.geojson", "mapa2.geojson"
//    }
}