DEMO VIDEO URL:
https://youtu.be/-wRnuDRIT18
<br>
<br>
SITE:
https://3.101.148.102:8443/manager/html
<br>
<br>
FILES WITH PREPARED STATEMENTS:<br>
ActorXMLParser.java<br>
ConfirmationServlet.java<br>
LoginServlet.java<br>
AddStarServlet.java<br>
SingleMovieServlet.java<br>
MoviesServlet.java<br>
PaymentServlet.java<br>
CartServlet.java<br>
SingleStarServlet.java<br>
IndexServlet.java<br>
CastXMLParser.java<br>
MovieXMLParser.java
<br>
<br>
OPTIMIZATION REPORT:<br>
Here are 3 optimizations I made for XML parsing…<br>
Hash Storage: Before parsing, loaded existing movie IDs, star names, and genre names into memory using HashSet and HashMap structures. This allowed fast checking of duplicates as it is O(1) each individual check with these data structures. Compared to comparing to the entire database fresh each time this ends up saving a lot of time.<br>
In-Depth Input Checking: By adding lots of early checks for malformed data, bad entries are skipped quickly without having to spend time going too deep into the program and attempting to add them to the database.
Batch Inputs: Instead of inserting every entry this was done in batches.
<br>
<br>
INCONSISTENCY ERROR REPORTS ARE IN src/PARSE BELOW IS WHAT TERMINAL PRINTED:<br>
ubuntu@ip-172-31-11-108:~/2025-spring-cs-122b-team24/Project3$ ./src/PARSE/run_parsers.sh<br>
Running ActorXMLParser...<br>
Inserted stars: 5999<br>
Duplicates skipped: 862<br>
Invalid names skipped: 2<br>
Running MovieXMLParser...<br>
Inserted movies: 12058<br>
Skipped invalid: 24<br>
Skipped duplicate: 28<br>
New genres: 108<br>
Running CastXMLParser...<br>
Linked stars to movies: 0<br>
Skipped missing: 48779<br>
Invalid names: 159<br>
Duplicate links skipped: 0<br>
Parsing complete. New logs written to *_errors.log
<br>
<br>
TEAM OF ONE



