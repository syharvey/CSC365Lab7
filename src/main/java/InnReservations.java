import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.lang.Integer;

/*
Introductory JDBC examples based loosely on the BAKERY dataset from CSC 365 labs.
 */
public class InnReservations {

    private final String JDBC_URL = "jdbc:h2:~/csc365_lab7";
    private final String JDBC_USER = "";
    private final String JDBC_PASSWORD = "";
    private String currentRoom;
    private ArrayList<String> currentList = new ArrayList<String>();
    private int monthCounter = 1;
    
    public static void main(String[] args) {
	try {
	    InnReservations ir = new InnReservations();
            ir.initDb();
	    ir.promptUser();
	    //ir.demo3();
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	}
    }
   
    private void promptUser() throws SQLException {
    	Scanner scanner = new Scanner(System.in);
	int option = 1;
	while (option != 6) {
	    System.out.print("\nEnter a menu option:\n\n [1] Get Rooms and Rates\n [2] Get Reservations\n [3] Make a Reservation Change\n [4] Cancel Your Reservation\n [5] Get Revenue Summary\n [6] Exit\n:-D ");
	    option = scanner.nextInt();
	    switch(option) {
	    	case 1:
		    FR1();	
		    break;
		case 2:
		    FR2();
		    break;
		case 3:
		    FR3();
		    break;
		case 4:
		    FR4();
		    break;
		case 5:
		    FR5();
		    break;
		case 6:
		    break;
		default:
		    System.out.println("Not a viable option. Please select option 1-6\n");
	    }
	}
    }
    
    // TODO: need to add next available checkin date and next available reservation
    private void FR1() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    String sql = "SELECT * from lab7_rooms order by RoomName";

	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

		while (rs.next()) {
		    String roomcode = rs.getString("RoomCode");
		    String roomname = rs.getString("RoomName");
		    int numbeds = rs.getInt("Beds");
		    String bedtype = rs.getString("bedType");
		    int maxocc = rs.getInt("maxOcc");
		    int baseprice = rs.getInt("basePrice");
		    String decor = rs.getString("decor");
		    System.out.format("%nRoomCode: %s%nRoomName: %s%nBeds: %d%nBedType: %s%nMaxOcc: %d%nBasePrice: %d%nDecor: %s%n", roomcode, roomname, numbeds, bedtype,maxocc,baseprice,decor);
		}
	    }

	}
    }

    private void FR2() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("First Name: ");
	    String firstName = scanner.nextLine();
	    System.out.print("Last Name: ");
	    String lastName = scanner.nextLine();
	    System.out.print("Room Code: ");
	    String roomCode = scanner.nextLine();
	    System.out.print("Desired Checkin (YYYY-MM-DD): ");
	    LocalDate checkIn = LocalDate.parse(scanner.nextLine());
	    System.out.print("Desired Checkout (YYYY-MM-DD): ");
	    LocalDate checkOut = LocalDate.parse(scanner.nextLine());
 	    System.out.print("Number of Children: ");
	    int numChildren = scanner.nextInt();
	    System.out.print("Number of Adults: ");
	    int numAdults = scanner.nextInt();

	    // TODO: check if reservation available
	    //if not, print out message and return
	    //if yes, update reservations table and print out confirmations
	    
	    boolean resAvailable = false;

	    if (resAvailable == false) {
	        System.out.println("Reservation could not be made\n");
	    	return;
	    }

	    // TODO: fix this to account for weekday/weekend day multipliers
	    long dateDiff = ChronoUnit.DAYS.between(checkIn, checkOut);
	    int numWeekdays = 0;
	    int numWeekend = 0;
	    double baseRate = 0.0;
	    double newRate = (double)numWeekdays * baseRate + (double)numWeekend * 1.1 * baseRate;
	    String roomName = "room name aqui";
	    String bedType = "bed type aqui";
	    String updateSql = "INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10105, ?, ?, ?, "+newRate+", ?, ?, ?, ?)";

	    // Step 3: Start transaction
	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
		
		// Step 4: Send SQL statement to DBMS
		pstmt.setString(1, roomCode);
		pstmt.setDate(2, java.sql.Date.valueOf(checkIn));
		pstmt.setDate(3, java.sql.Date.valueOf(checkOut));
		pstmt.setString(4, lastName);
		pstmt.setString(5, firstName);
		pstmt.setInt(6, numAdults);
		pstmt.setInt(7, numChildren);
		int rowCount = pstmt.executeUpdate();
		
		// Step 5: Handle results
		if (rowCount == 1) {
		    System.out.format("Reservation Confirmation%n Name: %s %s%n Room Code: %s%n Room Name: %s%n Bed Type: %s%n Checkin: _%n Checkout: _%n Adults: %d Children %d%n Total Cost: %f%n", firstName, lastName, roomCode, roomName, bedType, numAdults, numChildren, newRate);
		}
		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }
	}
    } 

    private void FR3() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("First Name: ");
	    String firstName = scanner.nextLine();
	    System.out.print("Last Name: ");
	    String lastName = scanner.nextLine();
	    System.out.print("\nYou may indicate 'no change' for the following fields:\nNew Checkin Date (YYYY-MM-DD): ");
	    String checkInStr = scanner.nextLine();
	    System.out.print("New Checkout Date (YYYY-MM-DD): ");
	    String checkOutStr = scanner.nextLine();
	    System.out.print("Number of Children: ");
	    String numChildrenStr = scanner.nextLine();
	    System.out.print("Number of Adults: ");
	    String numAdultsStr = scanner.nextLine();

	    LocalDate checkIn = LocalDate.MIN;
	    LocalDate checkOut = LocalDate.MAX;
	    int numChildren = 100; 
	    int numAdults = 100;
	    // get old reservation fields
	    String selectSql = "SELECT CheckIn, CheckOut, Kids, Adults FROM lab7_reservations where FirstName = "+firstName+" AND LastName = "+lastName;
	    
	    try (Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(selectSql)){
		while (rs.next()) {
		    checkIn = rs.getDate("CheckIn").toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		    checkOut = rs.getDate("CheckOut").toInstant().atZone(ZoneId.systemDefault()).toLocalDate();		    
		    numChildren = rs.getInt("Kids");
		    numAdults = rs.getInt("Adults");
		}
	    }
	    if (!checkInStr.equals("no change")) 
	    	checkIn = LocalDate.parse(checkInStr);
	    if (!checkOutStr.equals("no change"))
	    	checkOut = LocalDate.parse(checkOutStr);
	    if (!numChildrenStr.equals("no change"))
		numChildren = Integer.parseInt(numChildrenStr);
	    if (!numAdultsStr.equals("no change"))
		numAdults = Integer.parseInt(numAdultsStr);
	    // TODO:
	    // check if new checkin/checkout date is available

	    boolean resAvailable = false;
	    if (resAvailable == false) {
		System.out.println("We're sorry. Those reservation dates are unavailable\n");
		return;
	    }

	    String updateSql = "UPDATE lab7_reservations SET CheckIn = ?, CheckOut = ?, Kids = ?, Adults = ? WHERE FirstName = ? AND LastName = ?";

	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
		pstmt.setDate(1, java.sql.Date.valueOf(checkIn));
		pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
		pstmt.setInt(3, numChildren);
		pstmt.setInt(4, numAdults);
		pstmt.setString(5, firstName);
		pstmt.setString(6, lastName);
		int rowCount = pstmt.executeUpdate();
		
		if (rowCount == 1)
		    System.out.print("Updated your reservation%n");

		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }

	}
    }
    
    private void FR4() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("\nEnter your reservation code: ");
	    int code = scanner.nextInt();
	    
	    String selectSql = "SELECT * from lab7_reservations WHERE CODE = "+String.valueOf(code);

	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(selectSql)) {

		while (rs.next()) {
		    String room = rs.getString("Room");
		    Date checkIn = rs.getDate("CheckIn");
		    System.out.print("Are you sure you would like to cancel your reservation for "+room+" on "+checkIn+"? (yes/no): ");
		}
	    }
	    scanner.nextLine();
	    String response = scanner.nextLine();
	    if (response.equals("no"))
	        return;
	    String deleteSql = "DELETE FROM lab7_reservations WHERE CODE = ?";

	    // Step 3: Start transaction
	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
		
		// Step 4: Send SQL statement to DBMS
		pstmt.setInt(1, code);
		int rowCount = pstmt.executeUpdate();
		
		// Step 5: Handle results
		System.out.format("Removed %d record(s) for reservation %d%n", rowCount, code);

		// Step 6: Commit or rollback transaction
		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }

	}
    }

    private void outputRow() {
    	System.out.println(currentList);
	currentList.clear();
	monthCounter = 1;
    }

    private void makeRows(ArrayList<String> revInfo) {
    	String room = revInfo.get(0);
	// add case where monthCounter = 12 for different room than this room
	if (!room.equals(currentRoom)) {
	    outputRow();
	    currentRoom = room;
	    currentList.add(room);
	}
	int monthNo = Integer.parseInt(revInfo.get(1));
	String moRev = revInfo.get(2);
	while (monthNo != monthCounter) {
	    currentList.add("0.00");
	    monthCounter++;
	}
	currentList.add(moRev);
	if (monthCounter == 12) monthCounter = 1;
	else monthCounter++;
    }
    
    private void FR5() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    String selectSql = "select Room, month(CheckOut) as Month, round(sum(datediff(day, CheckIn, CheckOut)*Rate),0) as Rev from lab7_reservations join lab7_rooms on Room=RoomCode group by Room, month(CheckOut)";
	    //String selectSql = "SELECT Room, MONTHNAME(CheckOut) AS Month, ROUND(SUM(RATE*DATEDIFF(CheckOut, CheckIn)),0) AS Rev FROM lab7_reservations JOIN lab7_rooms ON Room=RoomCode GROUP BY Room, MONTHNAME(CheckOut)";
	    int i = 1;
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(selectSql)) {

		while (rs.next()) {
		    String room = rs.getString("Room");
		    if (i == 1) {
		        currentRoom = room;
			currentList.add(room);
		    }
		    String month = String.valueOf(rs.getInt("Month"));
		    String moRev = rs.getString("Rev");
		    ArrayList<String> revInfo = new ArrayList<String>();
		    revInfo.add(room);
		    revInfo.add(month);
		    revInfo.add(moRev);
		    makeRows(revInfo);
		    i++;
		    //System.out.println(room+" "+month+" "+moRev);
		}
	    }
	}
    }

    private void demo1() throws SQLException {

	// Step 0: Load JDBC Driver
	// No longer required as of JDBC 2.0  / Java 6
	try{
	    Class.forName("org.h2.Driver");
	    System.out.println("H2 JDBC Driver loaded");
	} catch (ClassNotFoundException ex) {
	    System.err.println("Unable to load JDBC Driver");
	    System.exit(-1);
	}

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    String sql = "ALTER TABLE hp_goods ADD COLUMN AvailUntil DATE";

	    // Step 3: (omitted in this example) Start transaction

	    try (Statement stmt = conn.createStatement()) {

		// Step 4: Send SQL statement to DBMS
		boolean exRes = stmt.execute(sql);
		
		// Step 5: Handle results
		System.out.format("Result from ALTER: %b %n", exRes);
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }
    

    // Demo2 - Establish JDBC connection, execute SELECT query, read & print result
    private void demo2() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    String sql = "SELECT * FROM hp_goods";

	    // Step 3: (omitted in this example) Start transaction

	    // Step 4: Send SQL statement to DBMS
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

		// Step 5: Receive results
		while (rs.next()) {
		    String flavor = rs.getString("Flavor");
		    String food = rs.getString("Food");
		    float price = rs.getFloat("Price");
		    System.out.format("%s %s ($%.2f) %n", flavor, food, price);
		}
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }


    // Demo3 - Establish JDBC connection, execute DML query (UPDATE)
    // -------------------------------------------
    // Never (ever) write database code like this!
    // -------------------------------------------
    private void demo3() throws SQLException {

        demo2();
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    String availUntilDate = scanner.nextLine();

	    // -------------------------------------------
	    // Never (ever) write database code like this!
	    // -------------------------------------------
	    String updateSql = "UPDATE hp_goods SET AvailUntil = '" + availUntilDate + "' " +
		               "WHERE Flavor = '" + flavor + "'";

	    // Step 3: (omitted in this example) Start transaction
	    
	    try (Statement stmt = conn.createStatement()) {
		
		// Step 4: Send SQL statement to DBMS
		int rowCount = stmt.executeUpdate(updateSql);
		
		// Step 5: Handle results
		System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);		
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	    
	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)

        demo2();
        
    }


    // Demo4 - Establish JDBC connection, execute DML query (UPDATE) using PreparedStatement / transaction    
    private void demo4() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    LocalDate availDt = LocalDate.parse(scanner.nextLine());
	    
	    String updateSql = "UPDATE hp_goods SET AvailUntil = ? WHERE Flavor = ?";

	    // Step 3: Start transaction
	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
		
		// Step 4: Send SQL statement to DBMS
		pstmt.setDate(1, java.sql.Date.valueOf(availDt));
		pstmt.setString(2, flavor);
		int rowCount = pstmt.executeUpdate();
		
		// Step 5: Handle results
		System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);

		// Step 6: Commit or rollback transaction
		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }

	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }



    // Demo5 - Construct a query using PreparedStatement
    private void demo5() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Find pastries with price <=: ");
	    Double price = Double.valueOf(scanner.nextLine());
	    System.out.print("Filter by flavor (or 'Any'): ");
	    String flavor = scanner.nextLine();

	    List<Object> params = new ArrayList<Object>();
	    params.add(price);
	    StringBuilder sb = new StringBuilder("SELECT * FROM hp_goods WHERE price <= ?");
	    if (!"any".equalsIgnoreCase(flavor)) {
		sb.append(" AND Flavor = ?");
		params.add(flavor);
	    }
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
		int i = 1;
		for (Object p : params) {
		    pstmt.setObject(i++, p);
		}

		try (ResultSet rs = pstmt.executeQuery()) {
		    System.out.println("Matching Pastries:");
		    int matchCount = 0;
		    while (rs.next()) {
			System.out.format("%s %s ($%.2f) %n", rs.getString("Flavor"), rs.getString("Food"), rs.getDouble("price"));
			matchCount++;
		    }
		    System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
		}
	    }

	}
    }


    private void initDb() throws SQLException {
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    try (Statement stmt = conn.createStatement()) {
		// create rooms table
                stmt.execute("DROP TABLE IF EXISTS lab7_rooms");
                stmt.execute("CREATE TABLE lab7_rooms (RoomCode char(5) PRIMARY KEY, RoomName varchar(30), Beds int(11), bedType varchar(8), maxOcc int(11), basePrice float, decor varchar(20))");
		stmt.execute("INSERT INTO lab7_rooms (RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('AOB', 'Abscond or bolster', 2, 'Queen', 4, 175, 'traditional')"); 
		stmt.execute("INSERT INTO lab7_rooms (RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('IBD', 'Immutable before decorum', 2, 'Queen', 4, 150, 'rustic')"); 
		stmt.execute("INSERT INTO lab7_rooms (RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('RND', 'Recluse and defiance', 1, 'King', 2, 150, 'modern')"); 
		stmt.execute("INSERT INTO lab7_rooms (RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('HBB', 'Harbinger but bequest', 1, 'Queen', 2, 100, 'modern')"); 
		stmt.execute("INSERT INTO lab7_rooms (RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) VALUES ('FNA', 'Frugal not apropos', 2, 'King', 4, 250, 'traditional')"); 
											
		// create reservations table
                stmt.execute("DROP TABLE IF EXISTS lab7_reservations");
                stmt.execute("CREATE TABLE lab7_reservations (CODE int(11) PRIMARY KEY, Room char(5), CheckIn date, CheckOut date, Rate float, LastName varchar(15), FirstName varchar(15), Adults int(11), Kids int(11))");
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10105, 'HBB', '2010-10-23', '2010-10-25', 100, 'SELBIG', 'CONRAD', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10183, 'IBD', '2010-09-19', '2010-09-20', 150, 'GABLER', 'DOLLIE', 2, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10449, 'RND', '2010-09-30', '2010-10-01', 150, 'KLESS', 'NELSON', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10489, 'AOB', '2010-02-02', '2010-02-05', 218.75, 'CARISTO', 'MARKITA', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10500, 'HBB', '2010-08-11', '2010-08-12', 90, 'YESSIOS', 'ANNIS', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10574, 'FNA', '2010-11-26', '2010-12-03', 287.5, 'SWEAZY', 'ROY', 2, 1)"); 


		// create goods table (just an example)
		stmt.execute("DROP TABLE IF EXISTS hp_goods");
                stmt.execute("CREATE TABLE hp_goods (GId varchar(15) PRIMARY KEY, Food varchar(100), Flavor varchar(100), Price DECIMAL(5,1), AvailUntil DATE)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('L1', 'Lemon', 'Cake', 20.0)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('L2', 'Lemon', 'Twist', 3.50)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('A3', 'Almond', 'Twist', 4.50)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('A4', 'Almond', 'Cookie', 4.50)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('L5', 'Lemon', 'Cookie', 1.50)");
                stmt.execute("INSERT INTO hp_goods (GId, Flavor, Food, Price) VALUES ('A6', 'Almond', 'Danish', 2.50)");
	    }
	}
    }
    

}

