# **Telegram Clone \- Desktop Messenger**

A professional, feature-rich desktop chat application inspired by Telegram. Built with JavaFX for a modern, aesthetic, and futuristic user interface, and powered by a robust PostgreSQL backend.

### **![Final UI Design](https://github.com/TheAmirrzh/telegram-clone/issues/1#issue-3390785865)**


## **Table of Contents**

1. [Description](https://www.google.com/search?q=%23description)  
2. [Features](https://www.google.com/search?q=%23features)  
3. [Tech Stack & Dependencies](https://www.google.com/search?q=%23tech-stack--dependencies)  
4. [Prerequisites](https://www.google.com/search?q=%23prerequisites)  
5. [How to Run From Scratch](https://www.google.com/search?q=%23how-to-run-from-scratch)  
6. [Safety Features](https://www.google.com/search?q=%23safety-features)  
7. [Changelog](https://www.google.com/search?q=%23changelog)  
8. [License](https://www.google.com/search?q=%23license)  
9. [Author](https://www.google.com/search?q=%23author)

## **Description**

This project is a high-fidelity desktop messaging client built as a final project for an advanced programming course. It emulates the core functionalities of Telegram, allowing users to communicate through private chats, participate in group conversations, and subscribe to broadcast channels.

The application is built entirely in Java, leveraging **JavaFX** for the GUI and **PostgreSQL** for persistent data storage. The project emphasizes a clean, object-oriented architecture, a secure backend, and a highly polished, modern user interface with both light and dark modes.

## **Features**

The application implements all mandatory features from the project specification, along with several bonus enhancements that improve the user experience.

### **Core Features (Mandatory)**

* **User Account Management**: Secure user registration and login with BCrypt password hashing.  
* **Profile Management**: Users can set their display name, a bio, and upload a custom profile picture.  
* **Contact Management**: A full-featured contact list where users can add, view, and remove contacts.  
* **Private (1-to-1) Chat**: Real-time, private messaging between two users with full chat history.  
* **Group Chat**:  
  * Create new groups and add members from the contact list.  
  * Real-time messaging within the group.  
  * View group member list and manage roles (admin/member).  
* **Channels**:  
  * Create broadcast channels where only the owner/admins can post.  
  * Users can subscribe to and view messages in channels.  
* **Global Search**: A unified search bar to discover and start conversations with new users, or find and join groups and channels.  
* **Image Messaging**: Send and receive images in all chat types.

### **Enhanced & Bonus Features**

* **Modern & Aesthetic UI/UX**: A complete redesign of the UI to be minimalistic, futuristic, and visually appealing.  
* **Light & Dark Modes**: A seamless theme toggle that switches the entire application between a light and dark aesthetic.  
* **Typing Indicators**: See when other users are typing in private and group chats.  
* **Message Replies**: Reply directly to specific messages to maintain context in conversations.  
* **Message Editing & Deletion**: Users can edit or delete their own messages after they have been sent.  
* **Group & Channel Administration**: Promote users to 'Admin' status to grant them additional privileges.

## **Tech Stack & Dependencies**

* **Language**: Java 17  
* **Framework**: JavaFX 20 for the graphical user interface.  
* **Database**: PostgreSQL 15 for all data persistence.  
* **Build Tool**: Apache Maven for dependency management and building the project.  
* **Database Pooling**: HikariCP for efficient and high-performance database connections.  
* **Password Hashing**: jBCrypt for secure password storage.

## **Prerequisites**

To run this project, you will need the following installed on your system:

* **Java Development Kit (JDK)**: Version 17 or later.  
* **Apache Maven**: Version 3.8 or later.  
* **Docker & Docker Compose**: The recommended way to run the PostgreSQL database.

## **How to Run From Scratch**

Follow these steps to get the application running on your local machine.

### **1\. Clone the Repository**

Open your terminal and clone this repository:
```bash

git clone https://github.com/TheAmirrzh/telegram-clone
cd telegram-clone
```

### **2\. Start the Database with Docker**

The project includes a docker-compose.yml file that will automatically set up the PostgreSQL database for you.
```bash
docker-compose up \-d
```

This command will start a PostgreSQL container on port 5432 and create the necessary database and user.

### **3\. Initialize the Database Schema**

A helper script is provided to create all the necessary tables and relationships.
```bash

chmod \+x scripts/init-db.sh  
./scripts/init-db.sh
```

This will execute the 01\_schema.sql file against the database.

### **4\. Run the Application**

Finally, use Maven to compile and run the JavaFX application.
```bash

mvn clean javafx:run
```

The application will launch, and you can now register a new account or log in with an existing one.

## **Safety Features**

Security was a key consideration during the development of this application.

* **Password Security**: All user passwords are not stored in plaintext. Instead, they are securely hashed using the industry-standard **BCrypt** algorithm before being stored in the database.  
* **SQL Injection Prevention**: All database queries are executed using PreparedStatement, which prevents SQL injection attacks by safely parameterizing all user inputs.

## **Changelog**

**Version 1.1 (September 2025\)**

* **UI Overhaul**: Redesigned the entire application UI for a modern, aesthetic, and futuristic user experience.  
* **Feature Enhancement**: Implemented a global search bar for users, groups, and channels.  
* **Bug Fixes**: Resolved critical bugs related to contact management, UI rendering, theme switching, and database schema mismatches.  
* **Code Polish**: Removed all debug statements and refactored key areas for improved clarity and performance.

**Version 1.0 (August 2025\)**

* Initial release with all mandatory features completed.  
* Implementation of private chats, groups, channels, and bonus features like message editing and replies.

## **License**

This project is licensed under the MIT License. See the LICENSE file for more details.

## **Author**

* **(AmirMohammad Gholami)**  [(TheAmirrzh](https://www.google.com/search?q=https://github.com/TheAmirrzh)
