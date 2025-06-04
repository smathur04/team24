# General
#### TEAM 24
#### SHAAN MATHUR
#### Project 5 Video Demo Link: https://drive.google.com/file/d/1J5-2g63eZEbSGiEWv9AVPJTE8r3MVGPj/view?usp=sharing

---

# Connection Pooling

#### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling.

- `Project4/WebContent/META-INF/context.xml`
- `Project4/src/AddMovieServlet.java`
- `Project4/src/AddStarServlet.java`
- `Project4/src/CartServlet.java`
- `Project4/src/ConfirmationServlet.java`
- `Project4/src/LoginServlet.java`
- `Project4/src/MetadataServlet.java`
- `Project4/src/MoviesServlet.java`
- `Project4/src/MovieSuggestionServlet.java`
- `Project4/src/PaymentServlet.java`
- `Project4/src/SingleMovieServlet.java`
- `Project4/src/SingleStarServlet.java`

#### Explain how Connection Pooling is utilized in the Fabflix code.

Connection pooling is implemented using Tomcat’s JDBC connection pool. In my `context.xml`, I defined two `Resource` entries: `jdbc/writeDB` and `jdbc/readDB`. These are used by each servlet to obtain pooled database connections.

Each servlet uses a class-level `DataSource dataSource` initialized in the `init()` method like this:

```java
dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB"); // or writeDB
```

Then, connections are retrieved via:

```java
Connection conn = dataSource.getConnection();
```

This setup allows efficient reuse of open connections, minimizes overhead, and improves scalability.

#### Explain how Connection Pooling works with two backend SQL.

Each backend Tomcat instance has a modified `context.xml` file with:
- `jdbc/writeDB`: always points to the **master** MySQL instance.
- `jdbc/readDB`: points to both the **master and the slave** using a MySQL `loadbalance://` JDBC URL.

This ensures that:
- Writes are always routed to the master.
- Reads are load-balanced between the master and the slave.
- Each instance manages its own connection pools.

---

# Master/Slave

#### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL.

- `Project4/WebContent/META-INF/context.xml`
- `Project4/src/AddMovieServlet.java`
- `Project4/src/AddStarServlet.java`
- `Project4/src/CartServlet.java`
- `Project4/src/ConfirmationServlet.java`
- `Project4/src/LoginServlet.java`
- `Project4/src/MetadataServlet.java`
- `Project4/src/MoviesServlet.java`
- `Project4/src/MovieSuggestionServlet.java`
- `Project4/src/PaymentServlet.java`
- `Project4/src/SingleMovieServlet.java`
- `Project4/src/SingleStarServlet.java`

#### How read/write requests were routed to Master/Slave SQL?

I route queries at the servlet level by choosing either `jdbc/writeDB` or `jdbc/readDB` in each servlet’s `init()` method.

- Servlets that perform **writes** (like `AddMovieServlet`, `AddStarServlet`, `ConfirmationServlet`, `PaymentServlet`) use `writeDB` to connect directly to the master.
- Servlets that perform **reads** (like `MoviesServlet`, `MovieSuggestionServlet`, `SingleMovieServlet`) use `readDB` to distribute load across master and slave.

MySQL master-slave replication ensures that read replicas stay up to date, while sticky sessions and Apache load balancing ensure consistent user routing across the cluster.
