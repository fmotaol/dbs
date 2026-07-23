package core;

public interface SavePointRestoreable extends Device {

	public void setAsDone();
	
	public boolean isDone();
	
	public String[] getSavePointColumns();
	
	public void restoreSavePointProperty(String property, String value);

	
}
