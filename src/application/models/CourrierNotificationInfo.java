package application.models;

import java.time.LocalDateTime;

public class CourrierNotificationInfo {
	private Courrier courrier;
    private boolean lu;
    private java.time.LocalDateTime dateNotification;
    private java.time.LocalDateTime dateLecture;
	
    public CourrierNotificationInfo() {
	}
    
    

	public Courrier getCourrier() {
		return courrier;
	}

	public void setCourrier(Courrier courrier) {
		this.courrier = courrier;
	}

	public boolean isLu() {
		return lu;
	}

	public void setLu(boolean lu) {
		this.lu = lu;
	}

	public java.time.LocalDateTime getDateNotification() {
		return dateNotification;
	}

	public void setDateNotification(java.time.LocalDateTime dateNotification) {
		this.dateNotification = dateNotification;
	}

	public java.time.LocalDateTime getDateLecture() {
		return dateLecture;
	}

	public void setDateLecture(java.time.LocalDateTime dateLecture) {
		this.dateLecture = dateLecture;
	}
    
    
    
    
}
