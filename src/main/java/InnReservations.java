import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.time.temporal.ChronoField;
import java.time.DayOfWeek;
import java.util.Calendar;
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
    private ArrayList<Integer> monthTotals = new ArrayList<Integer>();
    
    public static void main(String[] args) {
	try {
	    InnReservations ir = new InnReservations();
            ir.initDb();
	    ir.promptUser();
	    //ir.demo3();
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	} catch (InputMismatchException ime) {
	    System.err.println("InputMismatchException: Please type a valid character");
	}
    }
   
    private void promptUser() throws SQLException, InputMismatchException {
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
    
    private void FR1() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {

      Calendar c = Calendar.getInstance();
      java.sql.Date today = new java.sql.Date(c.getTime().getTime());

       String sql = 
    "SELECT RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor, Next, AvailNext FROM (" +          
             "SELECT RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor, MIN(CheckIn) AS Next " + 
             "from lab7_rooms LEFT OUTER JOIN lab7_reservations ON ((RoomCode = Room) AND (CheckIn > ?)) GROUP BY RoomCode"
   + ") AS A INNER JOIN (" + 
         "SELECT RoomCode AS Room, ? AS AvailNext FROM lab7_rooms LEFT OUTER JOIN lab7_reservations ON RoomCode = Room AND "
             + "? BETWEEN CheckIn AND CheckOut GROUP BY RoomCode, CheckIn HAVING CheckIn IS NULL" 
                 
             +" UNION "+

        "SELECT DISTINCT Room, CASE WHEN (MIN(CheckOut) OVER (PARTITION BY Room) = ?) then ? else (MIN(CheckOut) OVER (PARTITION BY Room)) end AS AvailNext FROM (" +
          "SELECT CODE, Room, CheckIn, CheckOut, IFNULL(DATEDIFF(d, CheckOut, Lead(CheckIn, 1) OVER (PARTITION BY Room ORDER BY CheckIn) ), 1) AS Days FROM lab7_reservations WHERE Room NOT IN ( "+
             "SELECT RoomCode FROM lab7_rooms LEFT OUTER JOIN lab7_reservations ON RoomCode = Room AND ? BETWEEN CheckIn AND CheckOut GROUP BY RoomCode, CheckIn HAVING CheckIn IS NULL) " +
             " ORDER BY Room, CheckIn " +
             ") AS T WHERE (? <= CheckOut) AND Days > 0"+
      ") AS B ON A.RoomCode = B.Room";

      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setDate(1,today);
      pstmt.setDate(2,today);
      pstmt.setDate(3,today);
      pstmt.setDate(4,today);
      pstmt.setDate(5,today);
      pstmt.setDate(6,today);
      pstmt.setDate(7,today);

      ResultSet rs = pstmt.executeQuery();

		while (rs.next()) {
		    String roomcode = rs.getString("RoomCode");
		    String roomname = rs.getString("RoomName");
		    int numbeds = rs.getInt("Beds");
		    String bedtype = rs.getString("bedType");
		    int maxocc = rs.getInt("maxOcc");
		    int baseprice = rs.getInt("basePrice");
		    String decor = rs.getString("decor");
          String nextCheckIn = rs.getString("AvailNext");
          if(nextCheckIn.equals(today.toString())){nextCheckIn = "TODAY";}
          String nextReser = rs.getString("Next");
          if(nextReser == null){ nextReser = "NONE";}

		    System.out.format("%nRoomCode: %s%nRoomName: %s%nBeds: %d%nBedType: %s%nMaxOcc: %d%nBasePrice: %d%nDecor: %s%nNext Check-In: %s%nNext Reservation: %s%n", 
                roomcode, roomname, numbeds, bedtype,maxocc,baseprice,decor,nextCheckIn,nextReser);
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

	    String roomName = "room name aqui";
	    String bedType = "bed type aqui";
	    double baseRate = 0.0;

       boolean isAvailable = true;
       boolean isLessThanMaxOcc = true;
       String sqlCheckDates = "SELECT CheckIn, CheckOut FROM lab7_reservations WHERE Room = ?";
       try(PreparedStatement pstmt = conn.prepareStatement(sqlCheckDates)){
         pstmt.setString(1, roomCode);
         ResultSet rs = pstmt.executeQuery();
         while(rs.next() && isAvailable){
            Date in = rs.getDate("CheckIn");
            Date out = rs.getDate("CheckOut");
         if(((in.compareTo(java.sql.Date.valueOf(checkIn)) <= 0  && java.sql.Date.valueOf(checkIn).compareTo(out) < 0)  && out.compareTo(java.sql.Date.valueOf(checkOut)) <= 0)
               || (in.compareTo(java.sql.Date.valueOf(checkIn)) <= 0  && out.compareTo(java.sql.Date.valueOf(checkOut)) > 0)
               || (in.compareTo(java.sql.Date.valueOf(checkOut)) < 0  && out.compareTo(java.sql.Date.valueOf(checkOut)) > 0)){
              
               isAvailable = false;
            } 
         }
       }

       String sqlCheckOcc = "SELECT RoomName, bedType, maxOcc, basePrice FROM lab7_Rooms WHERE RoomCode = ?";
       try(PreparedStatement pstmt = conn.prepareStatement(sqlCheckOcc)){
          int total = numChildren + numAdults;
          pstmt.setString(1, roomCode);
          ResultSet rs = pstmt.executeQuery();
          while(rs.next()){
            int occupancy = rs.getInt("maxOcc");
            roomName = rs.getString("RoomName");
            bedType = rs.getString("bedType");
            baseRate = rs.getFloat("basePrice");
            if(occupancy < total){
               isLessThanMaxOcc = false;
            }
          }
       }
	    
       if (isAvailable == false || isLessThanMaxOcc == false) {
	        System.out.println("Reservation could not be made\n");
	    }

       else{

   	   long dateDiff = ChronoUnit.DAYS.between(checkIn, checkOut);
         int numWeekend = getWeekEndDays(checkIn, checkOut);
	      int numWeekdays = (int)dateDiff - numWeekend;
         double newRate = (double)numWeekdays * baseRate + (double)numWeekend * 1.1 * baseRate;  
	      String updateSql = "INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (00000, ?, ?, ?, "+newRate+", ?, ?, ?, ?)";

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
		         System.out.format("%nReservation Confirmation%n Name: %s %s%n Room Code: %s%n Room Name: %s%n Bed Type: %s%n Checkin: %s%n Checkout: %s%n Adults: %d Children %d%n Total Cost: %.2f%n", firstName, lastName, roomCode, roomName, bedType, checkIn, checkOut, numAdults, numChildren, newRate);
		      }
         }	
      }
	}
} 

private int getWeekEndDays(LocalDate in, LocalDate out){
   int count = 0;
   long dateDiff = ChronoUnit.DAYS.between(in, out);
   for(int i = 1; i <= (int)dateDiff; i++){
      DayOfWeek day  = DayOfWeek.of((in.plusDays(i).get(ChronoField.DAY_OF_WEEK)));
      if(day.toString().equals("SATURDAY") || day.toString().equals("SUNDAY")){
         count++;
      }
   }
   return count;  
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
	while (monthCounter != 13) {
	    currentList.add("0");
	    monthCounter++;
	}
	System.out.print(currentList.get(0)+":");
	int total = 0;
	for (int i=1; i<13; i++) {
	    if (i == 1) System.out.format("%5s ", currentList.get(i));
	    else System.out.format("%7s", currentList.get(i));
	    int newMonthTotal = monthTotals.get(i-1) + Integer.parseInt(currentList.get(i));
	    monthTotals.set(i-1, newMonthTotal);
	    total += Integer.parseInt(currentList.get(i));
	}
	System.out.format("    %d%n", total);
	currentList.clear();
	monthCounter = 1;
    }

    private void makeRows(ArrayList<String> revInfo) {
    	String room = revInfo.get(0);
	if (!room.equals(currentRoom)) {
	    outputRow();
	    currentRoom = room;
	    currentList.add(room);
	}
	int monthNo = Integer.parseInt(revInfo.get(1));
	String moRev = revInfo.get(2);
	while (monthNo != monthCounter) {
	    currentList.add("0");
	    monthCounter++;
	}
	currentList.add(moRev);
	monthCounter++;
    }
    
    private void FR5() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    String selectSql = "select Room, month(CheckOut) as Month, round(sum(datediff(day, CheckIn, CheckOut)*Rate),0) as Rev from lab7_reservations join lab7_rooms on Room=RoomCode group by Room, month(CheckOut) order by Room";
	    int i = 1;
	    for (int j = 0; j < 12; j++)
		monthTotals.add(0);
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(selectSql)) {
		System.out.format("%9s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s   %6s%n", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Total");
		while (rs.next()) {
		    String room = rs.getString("Room");
		    if (i == 1) {
		        currentRoom = room;
			currentList.add(room);
		    }
		    String month = String.valueOf(rs.getInt("Month"));
		    String moRev = String.valueOf(rs.getInt("Rev"));
		    ArrayList<String> revInfo = new ArrayList<String>();
		    revInfo.add(room);
		    revInfo.add(month);
		    revInfo.add(moRev);
		    makeRows(revInfo);
		    i++;
		}
		System.out.format("%nTotals:%n    %5s ", String.valueOf(monthTotals.get(0)));
		for (int k=1; k<12; k++) {
	    	    System.out.format("%7s", String.valueOf(monthTotals.get(k)));
		}
		System.out.println();
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
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (99999, 'FNA', '2020-11-26', '2020-12-03', 287.5, 'VONHRESVELG', 'EDELGARD', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (91019, 'FNA', '2010-01-03', '2010-01-11', 287.5, 'VONRIEGAN', 'CLAUDE', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (20202, 'AOB', '2020-06-10', '2020-06-15', 287.5, 'BLAYDDID', 'DIMITRI', 2, 1)"); 





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

