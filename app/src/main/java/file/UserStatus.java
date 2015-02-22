package file;

public class UserStatus{
        private String username;
        private String role;
        private String status;
        private String handler;
        public UserStatus(){}

        public UserStatus(String username, String role, String status, String handler){
            this.username = username;
            this.role = role;
            this.status = status;
            this.handler = handler;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHandler() {
            return handler;
        }

        public void setHandler(String handler) {
            this.handler = handler;
        }
    }