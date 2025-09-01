package com.telegramapp.tools;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordTool {
    public static void main(String[] args){
        if (args.length==0) { System.out.println("Usage: PasswordTool <plain>"); return; }
        String hash = BCrypt.hashpw(args[0], BCrypt.gensalt());
        System.out.println(hash);
    }
}
