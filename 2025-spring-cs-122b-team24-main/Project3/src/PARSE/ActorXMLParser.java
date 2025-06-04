package PARSE;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.*;
import java.util.HashSet;

public class ActorXMLParser {
    private static Connection conn;
    private static HashSet<String> existingNames = new HashSet<>();
    private static int currentMaxId = 0;

    private static int insertedCount = 0;
    private static int skippedDuplicate = 0;
    private static int skippedInvalidName = 0;

    private static BufferedWriter errorLog;

    public static void main(String[] args) {
        try {
            errorLog = new BufferedWriter(new FileWriter("actor_errors.log", true));
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");

            loadExistingStars();
            parseActors(new File("actors63.xml"));

            conn.close();
            errorLog.close();

            System.out.printf("Inserted stars: %d\nDuplicates skipped: %d\nInvalid names skipped: %d\n",
                    insertedCount, skippedDuplicate, skippedInvalidName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadExistingStars() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars");
        while (rs.next()) {
            existingNames.add(rs.getString("name").toLowerCase().trim());
            int num = Integer.parseInt(rs.getString("id").replace("nm", ""));
            if (num > currentMaxId) currentMaxId = num;
        }
        rs.close();
    }

    private static void parseActors(File xmlFile) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), "ISO-8859-1");
            InputSource is = new InputSource(reader);
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            NodeList actorList = doc.getElementsByTagName("actor");
            for (int i = 0; i < actorList.getLength(); i++) {
                Element actorElem = (Element) actorList.item(i);
                String name = getTextValue(actorElem, "stagename");
                String dobStr = getTextValue(actorElem, "dob");

                if (name == null || name.trim().isEmpty()) {
                    errorLog.write("Skipped actor: Missing or empty name.\n");
                    skippedInvalidName++;
                    continue;
                }

                name = name.trim().replaceAll("\\s+", " ");
                if (name.length() < 3) {
                    errorLog.write("Skipped actor: name too short — '" + name + "'\n");
                    skippedInvalidName++;
                    continue;
                }
                if (name.matches(".*\\d.*")) {
                    errorLog.write("Skipped actor: name contains digits — '" + name + "'\n");
                    skippedInvalidName++;
                    continue;
                }
                if (name.matches("[^a-zA-Z ].*")) {
                    errorLog.write("Skipped actor: invalid characters — '" + name + "'\n");
                    skippedInvalidName++;
                    continue;
                }

                String canonicalName = name.toLowerCase();
                if (existingNames.contains(canonicalName)) {
                    errorLog.write("Skipped actor: duplicate name — '" + name + "'\n");
                    skippedDuplicate++;
                    continue;
                }

                Integer birthYear = null;
                try {
                    birthYear = Integer.parseInt(dobStr);
                } catch (Exception e) {
                    if (dobStr != null) {
                        errorLog.write("Skipped actor: invalid birth year — '" + dobStr + "' for '" + name + "'\n");
                    }
                }

                String newId = String.format("nm%07d", ++currentMaxId);
                insertStar(newId, name, birthYear);
                existingNames.add(canonicalName);
                insertedCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertStar(String id, String name, Integer birthYear) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)");
        ps.setString(1, id);
        ps.setString(2, name);
        if (birthYear != null) {
            ps.setInt(3, birthYear);
        } else {
            ps.setNull(3, Types.INTEGER);
        }
        ps.executeUpdate();
        ps.close();
    }

    private static String getTextValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) return null;
        return nodeList.item(0).getTextContent().trim();
    }
}
