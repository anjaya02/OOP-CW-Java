public abstract class User {
    protected final int id;
    protected String name;
    protected String email;
    protected String password;
    protected String mobileNumber;

    public User(int id, String name, String email, String password, String mobileNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobileNumber = mobileNumber;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean validatePassword(String inputPassword) {
        return password.equals(inputPassword);
    }

    public String getEmail() {
        return email;
    }

}
