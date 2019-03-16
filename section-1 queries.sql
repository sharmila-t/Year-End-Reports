USE csci3901;

/*
a. Which products have been ordered and will have an inventory below their reorder level even after their current orders arrive? 
*/

 SELECT 
    *
FROM
    products
WHERE
    (UnitsInStock + UnitsOnOrder) < ReorderLevel
        AND NOT (Discontinued);

/*
b. Which orders can we not ship in the future because the orders contain discontinued products?  
*/

SELECT 
    orders.*
FROM
    orders
        LEFT JOIN
    orderdetails ON orders.OrderID = orderdetails.OrderID
WHERE
    orderdetails.ProductID IN (SELECT 
            ProductID
        FROM
            products
        WHERE
            Discontinued)
        AND ShippedDate IS NULL;
        
/*
c.  What are the sales by region (in dollar values) for 1997?
*/

/* sales by customer region */
SELECT 
    IF(Region IS NULL, 'Others', Region) AS Region,
    SUM(salesdetails.Total) AS Sales
FROM
    (SELECT 
        customerID, ((UnitPrice * Quantity) - Discount) AS Total
    FROM
        orders
    LEFT JOIN orderdetails ON orders.OrderID = orderdetails.OrderID
    WHERE
        YEAR(orders.OrderDate) = 1997) AS salesdetails
        LEFT JOIN
    customers ON salesdetails.customerID = customers.customerID
GROUP BY customers.Region;

/* sales by store region */
SELECT 
    region.RegionDescription AS Region,
    SUM(salesdetails.Total) AS Sales
FROM
    (SELECT 
        EmployeeID, ((UnitPrice * Quantity) - Discount) AS Total
    FROM
        orders
    LEFT JOIN orderdetails ON orders.OrderID = orderdetails.OrderID
    WHERE
        YEAR(orders.OrderDate) = 1997) AS salesdetails
        LEFT JOIN
    employees ON salesdetails.EmployeeID = employees.EmployeeID
        LEFT JOIN
    employeeterritories ON employees.EmployeeID = employeeterritories.EmployeeID
        LEFT JOIN
    territories ON territories.TerritoryID = employeeterritories.TerritoryID
        LEFT JOIN
    region ON region.RegionID = territories.RegionID
GROUP BY region.RegionID;

/*
d. Who are the 5 suppliers of products whose products we resell the most (top 5 by sales value) ?
*/

SELECT 
    suppliers.*
FROM
    suppliers
        RIGHT JOIN
    (SELECT DISTINCT
        SupplierID
    FROM
        products
    RIGHT JOIN (SELECT 
        COUNT(OrderID) AS OrderCount, ProductID
    FROM
        orderdetails
    GROUP BY ProductID) AS ordercountdetails ON products.ProductID = ordercountdetails.ProductID
    ORDER BY OrderCount DESC
    LIMIT 5) AS topsuppliers ON suppliers.SupplierID = topsuppliers.SupplierID;
    
/*
e. What is the average number of days, for each employee, between the time they record an order and the time the order is shipped?
*/

SELECT 
    employees.EmployeeID,
    FirstName,
    LastName,
    avgdays AS 'Average Shipment Days'
FROM
    (SELECT 
        AVG(DATEDIFF(ShippedDate, OrderDate)) AS avgdays, EmployeeID
    FROM
        orders
    WHERE
        ShippedDate IS NOT NULL
    GROUP BY EmployeeID) AS emp
        LEFT JOIN
    employees ON emp.EmployeeID = employees.EmployeeID;

