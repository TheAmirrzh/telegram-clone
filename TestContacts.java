import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.User;
import java.sql.SQLException;
import java.util.List;

public class TestContacts {
    public static void main(String[] args) {
        try {
            UserDAOImpl userDAO = new UserDAOImpl();
            
            // Test loading user-1
            System.out.println("=== Testing contact loading ===");
            User user1 = userDAO.findById("user-1").orElse(null);
            if (user1 != null) {
                System.out.println("User-1 found: " + user1.getUsername());
                System.out.println("User-1 contact IDs: " + user1.getContactIds());
                
                // Test getting contacts
                List<User> contacts = userDAO.getContacts("user-1");
                System.out.println("Contacts for user-1: " + contacts.size());
                for (User contact : contacts) {
                    System.out.println("  - " + contact.getUsername() + " (" + contact.getDisplayName() + ")");
                }
            } else {
                System.out.println("User-1 not found!");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
