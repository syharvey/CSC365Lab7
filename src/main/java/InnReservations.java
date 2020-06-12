// CSC 365 Lab 7
// Syrsha Harvey and Kattia Chang-Kam
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

    private boolean checkNewRes(Connection conn, String room, LocalDate checkIn, 
	    LocalDate checkOut) throws SQLException {
	String checkInStr = checkIn.toString();
	String checkOutStr = checkOut.toString();
	int code = 0;
   	String selectSql = "SELECT * FROM lab7_reservations WHERE Room = '"+room+"' AND ((CheckOut >= '"+checkInStr+"' AND CheckIn <= '"+checkInStr+"') OR (CheckOut >= '"+checkOutStr+"' AND CheckIn <= '"+checkOutStr+"') OR (CheckIn >= '"+checkInStr+"' AND CheckOut <= '"+checkOutStr+"'))";
	try (Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery(selectSql)){
	    while (rs.next()) {
		code = rs.getInt("CODE");
	    }
	}
	if (code == 0) return true;
	else return false;
    }

    private void FR3() throws SQLException {

	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
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
	    String room = "";
	    // get old reservation fields
	    String selectSql = "SELECT * FROM lab7_reservations WHERE FirstName = '"+firstName+"' AND LastName = '"+lastName+"'";
	    
	    try (Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(selectSql)){
		while (rs.next()) {
		    checkIn = rs.getDate("CheckIn").toLocalDate();
		    checkOut = rs.getDate("CheckOut").toLocalDate();		    
		    numChildren = rs.getInt("Kids");
		    numAdults = rs.getInt("Adults");
		    room = rs.getString("Room");
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
	    
	    boolean resAvailable = true;
	    if (!(checkInStr.equals("no change") && checkOutStr.equals("no change")))
		resAvailable = checkNewRes(conn, room, checkIn, checkOut);
	    if (resAvailable == false) {
		System.out.println("\nWe're sorry. Those reservation dates are unavailable for room: "+room+"\n");
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
		    System.out.println("\nUpdated your reservation\n");
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
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10105, 'HBB', '2020-10-23', '2020-10-25', 100, 'SELBIG', 'CONRAD', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10183, 'IBD', '2020-09-19', '2020-09-20', 150, 'GABLER', 'DOLLIE', 2, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10449, 'RND', '2020-09-30', '2020-10-01', 150, 'KLESS', 'NELSON', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10489, 'AOB', '2020-02-02', '2020-02-05', 218.75, 'CARISTO', 'MARKITA', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10500, 'HBB', '2020-08-11', '2020-08-12', 90, 'YESSIOS', 'ANNIS', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (10574, 'AOB', '2020-11-26', '2020-12-03', 287.5, 'SWEAZY', 'ROY', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (99999, 'FNA', '2020-11-26', '2020-12-03', 287.5, 'VONHRESVELG', 'EDELGARD', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (14839, 'RND', '2020-06-10', '2020-06-16', 150, 'SENG', 'PAPA', 1, 0)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (91019, 'FNA', '2010-01-03', '2010-01-11', 287.5, 'VONRIEGAN', 'CLAUDE', 2, 1)"); 
		stmt.execute("INSERT INTO lab7_reservations (CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) VALUES (20202, 'AOB', '2020-06-10', '2020-06-15', 287.5, 'BLAYDDID', 'DIMITRI', 2, 1)"); 

	    }
	}
    }
    

}

