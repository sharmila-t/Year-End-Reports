package northwind;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.sql.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class Main {
	static Document document;

	public static void main(String args[]) throws Exception {
		String StartDate = "1996-01-01";
		String EndDate = "1996-08-01";
		
		Class.forName("com.mysql.cj.jdbc.Driver");
		String URL = "jdbc:mysql://localhost:3306";
		String user = "root";
		String pwd = "root";
		Connection con = DriverManager.getConnection(URL, user, pwd);
		
		
		document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element summary = document.createElement("year_end_summary");
		document.appendChild(summary);
		
		Element year = document.createElement("year");
		year.appendChild(createNode("start_date", StartDate));
		year.appendChild(createNode("end_date", EndDate));
		summary.appendChild(year);
		
		try {
			Statement stmt = con.createStatement();
			stmt.executeQuery("USE class_3901;");

			String cus_query = new String(Files.readAllBytes(Paths.get("customer.txt")), "UTF-8");
			PreparedStatement customer=con.prepareStatement(cus_query);  
			customer.setDate(1, java.sql.Date.valueOf(StartDate));
			customer.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet cus_report = customer.executeQuery();
			CreateCustomerObject(cus_report, summary);


			String product_query = new String(Files.readAllBytes(Paths.get("product.txt")), "UTF-8");
			PreparedStatement product=con.prepareStatement(product_query);  
			product.setDate(1, java.sql.Date.valueOf(StartDate));
			product.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet pdt_report = product.executeQuery();
			CreateProductObject(pdt_report, summary);
			
			
			String supplier_query = new String(Files.readAllBytes(Paths.get("supplier.txt")), "UTF-8");
			PreparedStatement supplier=con.prepareStatement(supplier_query);  
			supplier.setDate(1, java.sql.Date.valueOf(StartDate));
			supplier.setDate(2, java.sql.Date.valueOf(EndDate));
			ResultSet supplier_report = supplier.executeQuery();
			CreateSupplierObject(supplier_report, summary);

			writeToXML();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			con.close();
		}
	}



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

		summary.appendChild(productList);
	}


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

	
	static Element createAdressNode(ResultSet result) throws DOMException, SQLException {
		Element address = document.createElement("address");			
		address.appendChild(createNode("street_address", result.getString("Address")));
		address.appendChild(createNode("city", result.getString("City")));
		address.appendChild(createNode("region", result.getString("Region")));
		address.appendChild(createNode("postal_code", result.getString("PostalCode")));
		address.appendChild(createNode("country", result.getString("Country")));
		return address;
	}
	
	static Element createNode(String nodeName, String value) {
		Element Node = document.createElement(nodeName);
		Node.setTextContent(value);
		return Node;
	}

	public static void writeToXML() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource domSource = new DOMSource(document);
		StreamResult streamResult = new StreamResult(new File("output.xml"));
		transformer.transform(domSource, streamResult);
	}
}
