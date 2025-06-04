package PARSE;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;

public class CastXMLParser {
    private static Connection conn;
    private static HashMap<String, String> movieIdMap = new HashMap<>();
    private static HashMap<String, String> starIdMap = new HashMap<>();
    private static HashSet<String> existingLinks = new HashSet<>();

    private static int linkedCount = 0;
    private static int skippedMissing = 0;
    private static int skippedInvalidName = 0;
    private static int skippedDuplicateLink = 0;

    private static BufferedWriter errorLog;

    public static void main(String[] args) {
        try {
            errorLog = new BufferedWriter(new FileWriter("cast_errors.log", true));
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            conn.setAutoCommit(false);

            loadExistingData();
            parseCasts(new File("casts124.xml"));

            conn.commit();
            conn.close();
            errorLog.close();

            System.out.printf("Linked stars to movies: %d\nSkipped missing: %d\nInvalid names: %d\nDuplicate links skipped: %d\n",
                    linkedCount, skippedMissing, skippedInvalidName, skippedDuplicateLink);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadExistingData() throws SQLException {
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT id FROM movies");
        while (rs.next()) movieIdMap.put(rs.getString("id"), rs.getString("id"));
        rs.close();

        rs = stmt.executeQuery("SELECT id, name FROM stars");
        while (rs.next()) starIdMap.put(rs.getString("name").toLowerCase().trim(), rs.getString("id"));
        rs.close();

        rs = stmt.executeQuery("SELECT starId, movieId FROM stars_in_movies");
        while (rs.next()) {
            String link = rs.getString("starId") + "_" + rs.getString("movieId");
            existingLinks.add(link);
        }
        rs.close();
    }

    private static void parseCasts(File xmlFile) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), "ISO-8859-1");
            InputSource is = new InputSource(reader);
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            NodeList mList = doc.getElementsByTagName("m");

            PreparedStatement ps = conn.prepareStatement("INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)");

            int batchSize = 500;
            int batchCount = 0;

            for (int i = 0; i < mList.getLength(); i++) {
                Element mElem = (Element) mList.item(i);
                String movieId = getTextValue(mElem, "f");
                String starName = getTextValue(mElem, "a");

                if (starName == null || starName.trim().isEmpty()) {
                    errorLog.write("Skipped link: missing star name (movie ID: " + movieId + ")\n");
                    skippedInvalidName++;
                    continue;
                }

                starName = starName.trim().replaceAll("\\s+", " ");
                if (starName.length() < 3) {
                    errorLog.write("Skipped link: star name too short — '" + starName + "' (movie ID: " + movieId + ")\n");
                    skippedInvalidName++;
                    continue;
                }
                if (starName.matches(".*\\d.*")) {
                    errorLog.write("Skipped link: star name has digits — '" + starName + "' (movie ID: " + movieId + ")\n");
                    skippedInvalidName++;
                    continue;
                }
                if (starName.matches("[^a-zA-Z ].*")) {
                    errorLog.write("Skipped link: star name has invalid characters — '" + starName + "' (movie ID: " + movieId + ")\n");
                    skippedInvalidName++;
                    continue;
                }

                String canonicalName = starName.toLowerCase();
                if (!movieIdMap.containsKey(movieId)) {
                    errorLog.write("Skipped link: movie ID not found — '" + movieId + "' for star '" + starName + "'\n");
                    skippedMissing++;
                    continue;
                }
                if (!starIdMap.containsKey(canonicalName)) {
                    errorLog.write("Skipped link: star not found — '" + starName + "' (movie ID: " + movieId + ")\n");
                    skippedMissing++;
                    continue;
                }

                String starId = starIdMap.get(canonicalName);
                String linkKey = starId + "_" + movieId;

                if (existingLinks.contains(linkKey)) {
                    errorLog.write("Skipped link: duplicate relationship (starId=" + starId + ", movieId=" + movieId + ")\n");
                    skippedDuplicateLink++;
                    continue;
                }

                ps.setString(1, starId);
                ps.setString(2, movieId);
                ps.addBatch();
                existingLinks.add(linkKey);
                linkedCount++;
                batchCount++;

                if (batchCount % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }

            if (batchCount % batchSize != 0) {
                ps.executeBatch();
                conn.commit();
            }

            ps.close();
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
