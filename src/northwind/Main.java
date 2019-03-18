package northwind;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.sql.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import northwind.MyIdentity;


public class Main {
	// XML DOM Object
	static Document document;

	public static void main(String args[]) throws Exception {
		Scanner scan = new Scanner(System.in);
		System.out.println("Start date?");
		String StartDate = scan.nextLine();
		System.out.println("End date?");
		String EndDate = scan.nextLine();
		System.out.println("Output filename?");
		String output = scan.nextLine();
		scan.close();

		document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element summary = document.createElement("year_end_summary");
		document.appendChild(summary);

		Element year = document.createElement("year");
		year.appendChild(createNode("start_date", StartDate));
		year.appendChild(createNode("end_date", EndDate));
		summary.appendChild(year);
		
		Class.forName("com.mysql.cj.jdbc.Driver");
		String URL = "jdbc:mysql://db.cs.dal.ca:3306";
		
		//Gets and sets username and password from properties file
		Properties id = new Properties();        
		MyIdentity.setIdentity( id );
		
		// Connection String
		Connection con = DriverManager.getConnection(URL, id.getProperty("user"), id.getProperty("pwd"));

		try {
			Statement stmt = con.createStatement();
			stmt.executeQuery("USE "+ id.getProperty("database"));
			
			// Customer summary
			String cus_query = new String(Files.readAllBytes(Paths.get("customer.txt")), "UTF-8");
			PreparedStatement customer=con.prepareStatement(cus_query);  
			customer.setDate(1, java.sql.Date.valueOf(StartDate));
			customer.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet cus_report = customer.executeQuery();
			CreateCustomerObject(cus_report, summary);

			// Product summary by category
			String product_query = new String(Files.readAllBytes(Paths.get("product.txt")), "UTF-8");
			PreparedStatement product=con.prepareStatement(product_query);  
			product.setDate(1, java.sql.Date.valueOf(StartDate));
			product.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet pdt_report = product.executeQuery();
			CreateProductObject(pdt_report, summary);

			// Supplier summary
			String supplier_query = new String(Files.readAllBytes(Paths.get("supplier.txt")), "UTF-8");
			PreparedStatement supplier=con.prepareStatement(supplier_query);  
			supplier.setDate(1, java.sql.Date.valueOf(StartDate));
			supplier.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet supplier_report = supplier.executeQuery();
			CreateSupplierObject(supplier_report, summary);

			writeToXML(output);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			con.close();
		}
	}


	// Constructs XML object from  product resultset
	public static void CreateProductObject(ResultSet result, Element summary) throws Exception{
		Map<String, Element> categoryMap= new HashMap<String, Element>();
		Element productList = document.createElement("product_list");
		
		while (result.next()) {
			Element productNode = document.createElement("product");
			productNode.appendChild(createNode("product_name", result.getString("ProductName")));
			productNode.appendChild(createNode("supplier_name", result.getString("CompanyName")));
			productNode.appendChild(createNode("units_sold", result.getString("Quantity")));
			productNode.appendChild(createNode("sale_value", result.getString("Total")));

			String category = result.getString("CategoryName");
			if(categoryMap.containsKey(category)) {
				categoryMap.get(category).appendChild(productNode);
			} else {
				Element categoryNode = document.createElement("category");
				Element categoryName = createNode("category_name", category);
				categoryNode.appendChild(categoryName);
				categoryNode.appendChild(productNode);
				productList.appendChild(categoryNode);
				categoryMap.put(category, categoryNode);
			}

		}
		
		//appending to summary list
		summary.appendChild(productList);
	}

	// Constructs XML object from  customer resultset
	public static void CreateCustomerObject(ResultSet result, Element summary) throws Exception {
		Element root = document.createElement("customer_list");

		while (result.next()) {
			Element customer = document.createElement("customer");			
			customer.appendChild(createNode("customer_name", result.getString("CompanyName")));
			customer.appendChild(createAdressNode(result));
			customer.appendChild(createNode("num_orders", result.getString("OrdersCount")));
			customer.appendChild(createNode("order_value", result.getString("TotalSales")));
			root.appendChild(customer);
		}

		summary.appendChild(root);
	}

	// Constructs XML object from  product resultset
	public static void CreateSupplierObject(ResultSet result, Element summary) throws Exception {
		Element root = document.createElement("supplier_list");

		while (result.next()) {
			Element supplier = document.createElement("supplier");			
			supplier.appendChild(createNode("supplier_name", result.getString("CompanyName")));
			supplier.appendChild(createAdressNode(result));
			supplier.appendChild(createNode("num_products", result.getString("Quantity")));
			supplier.appendChild(createNode("product_value", result.getString("Total")));
			root.appendChild(supplier);
		}

		summary.appendChild(root);
	}

	// Constructs XML object in typical address format
	static Element createAdressNode(ResultSet result) throws DOMException, SQLException {
		Element address = document.createElement("address");			
		address.appendChild(createNode("street_address", result.getString("Address")));
		address.appendChild(createNode("city", result.getString("City")));
		address.appendChild(createNode("region", result.getString("Region")));
		address.appendChild(createNode("postal_code", result.getString("PostalCode")));
		address.appendChild(createNode("country", result.getString("Country")));
		return address;
	}
	
	// Creates a XML data Node
	static Element createNode(String nodeName, String value) {
		Element Node = document.createElement(nodeName);
		Node.setTextContent(value);
		return Node;
	}
	
	// Writes output to given XML file
	public static void writeToXML(String output) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource domSource = new DOMSource(document);
		StreamResult streamResult = new StreamResult(new File(output));
		transformer.transform(domSource, streamResult);
	}
}
