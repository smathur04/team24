package PARSE;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.*;
import java.util.HashMap;

public class MovieXMLParser {
    private static Connection conn;
    private static HashMap<String, String> existingMovies = new HashMap<>();
    private static HashMap<String, Integer> genreMap = new HashMap<>();
    private static int currentMaxId = 0;

    private static int insertedCount = 0;
    private static int skippedInvalid = 0;
    private static int skippedDuplicate = 0;
    private static int newGenres = 0;

    private static BufferedWriter errorLog;

    public static void main(String[] args) {
        try {
            errorLog = new BufferedWriter(new FileWriter("movie_errors.log", true));
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            conn.setAutoCommit(false);

            loadExistingMovies();
            loadGenres();
            parseMovies(new File("mains243.xml"));

            conn.commit();
            conn.close();
            errorLog.close();

            System.out.printf("Inserted movies: %d\nSkipped invalid: %d\nSkipped duplicate: %d\nNew genres: %d\n",
                    insertedCount, skippedInvalid, skippedDuplicate, newGenres);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadExistingMovies() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, title, year, director FROM movies");
        while (rs.next()) {
            String key = rs.getString("title").toLowerCase().trim() + "_" + rs.getInt("year") + "_" + rs.getString("director").toLowerCase().trim();
            existingMovies.put(key, rs.getString("id"));
            int num = Integer.parseInt(rs.getString("id").replace("tt", ""));
            if (num > currentMaxId) currentMaxId = num;
        }
        rs.close();
    }

    private static void loadGenres() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM genres");
        while (rs.next()) {
            genreMap.put(rs.getString("name").toLowerCase().trim(), rs.getInt("id"));
        }
        rs.close();
    }

    private static void parseMovies(File xmlFile) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), "ISO-8859-1");
            InputSource is = new InputSource(reader);
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            NodeList directorFilmsList = doc.getElementsByTagName("directorfilms");

            PreparedStatement insertMovie = conn.prepareStatement("INSERT INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ?)");
            PreparedStatement insertGenre = conn.prepareStatement("INSERT INTO genres (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement insertLink = conn.prepareStatement("INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)");

            int batchCount = 0;
            int batchSize = 500;

            for (int i = 0; i < directorFilmsList.getLength(); i++) {
                Element dfElem = (Element) directorFilmsList.item(i);
                String director = getTextValue(dfElem, "dirname");
                if (director == null || director.trim().isEmpty()) continue;
                director = director.trim();

                NodeList films = dfElem.getElementsByTagName("film");
                for (int j = 0; j < films.getLength(); j++) {
                    Element filmElem = (Element) films.item(j);
                    String fid = filmElem.getAttribute("fid");
                    String title = getTextValue(filmElem, "t");
                    String yearStr = getTextValue(filmElem, "year");
                    String priceStr = getTextValue(filmElem, "price");

                    if (title == null || title.trim().isEmpty() || yearStr == null || director == null) {
                        errorLog.write("Invalid movie data: title or year missing — FID: " + fid + "\n");
                        skippedInvalid++;
                        continue;
                    }

                    int year;
                    try {
                        year = Integer.parseInt(yearStr);
                    } catch (Exception e) {
                        errorLog.write("Invalid year for film " + fid + ": " + yearStr + "\n");
                        skippedInvalid++;
                        continue;
                    }

                    float price = 9.99f;
                    try {
                        if (priceStr != null && !priceStr.isEmpty()) {
                            price = Float.parseFloat(priceStr);
                        }
                    } catch (Exception e) {
                        // keep default
                    }

                    String key = title.toLowerCase().trim() + "_" + year + "_" + director.toLowerCase().trim();
                    if (existingMovies.containsKey(key)) {
                        errorLog.write("Duplicate movie: " + title + " (" + year + ")\n");
                        skippedDuplicate++;
                        continue;
                    }

                    String newId = String.format("tt%07d", ++currentMaxId);
                    insertMovie.setString(1, newId);
                    insertMovie.setString(2, title);
                    insertMovie.setInt(3, year);
                    insertMovie.setString(4, director);
                    insertMovie.setFloat(5, price);
                    insertMovie.addBatch();
                    existingMovies.put(key, newId);
                    insertedCount++;
                    batchCount++;

                    Element catsElem = (Element) filmElem.getElementsByTagName("cats").item(0);
                    if (catsElem != null) {
                        NodeList catList = catsElem.getElementsByTagName("cat");
                        for (int c = 0; c < catList.getLength(); c++) {
                            String genreName = catList.item(c).getTextContent().trim();
                            if (genreName.isEmpty()) continue;

                            int genreId;
                            String genreKey = genreName.toLowerCase();
                            if (genreMap.containsKey(genreKey)) {
                                genreId = genreMap.get(genreKey);
                            } else {
                                insertGenre.setString(1, genreName);
                                insertGenre.executeUpdate();
                                ResultSet rs = insertGenre.getGeneratedKeys();
                                if (rs.next()) {
                                    genreId = rs.getInt(1);
                                    genreMap.put(genreKey, genreId);
                                    newGenres++;
                                } else {
                                    continue;
                                }
                                rs.close();
                            }

                            insertLink.setInt(1, genreId);
                            insertLink.setString(2, newId);
                            insertLink.addBatch();
                        }
                    }

                    if (batchCount % batchSize == 0) {
                        insertMovie.executeBatch();
                        insertLink.executeBatch();
                        conn.commit();
                    }
                }
            }

            insertMovie.executeBatch();
            insertLink.executeBatch();
            conn.commit();

            insertMovie.close();
            insertGenre.close();
            insertLink.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getTextValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) return null;
        return nodeList.item(0).getTextContent().trim();
    }
}
