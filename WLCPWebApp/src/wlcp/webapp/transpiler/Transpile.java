package wlcp.webapp.transpiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.persistence.config.PersistenceUnitProperties;

/**
 * Servlet implementation class Transpile
 */
@WebServlet("/Transpile")
public class Transpile extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private EntityManagerFactory entityManagerFactory = null;
	private EntityManager entityManager = null;
	//private JavaScriptTranspiler transpiler = null;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Transpile() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		
		initJPA();
		
		//transpiler = new JavaScriptTranspiler(entityManager);
		//transpiler.Transpile("test");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	private void initJPA() {
		Map<Object, Object> properties = new HashMap<Object, Object>();
		properties.put(PersistenceUnitProperties.JDBC_URL, "jdbc:mysql://localhost/test");
		properties.put(PersistenceUnitProperties.JDBC_DRIVER, "com.mysql.jdbc.Driver");
		properties.put(PersistenceUnitProperties.JDBC_USER, "wlcp");
		properties.put(PersistenceUnitProperties.JDBC_PASSWORD, "wlcp");
		properties.put("eclipselink.ddl-generation", "none");
		properties.put("eclipselink.target-database", "MySQL");
        
        //Create our factory
        entityManagerFactory = Persistence.createEntityManagerFactory("WLCPDataModel", properties);
        
        //Setup our entity manager
        entityManager = entityManagerFactory.createEntityManager();
	}

}