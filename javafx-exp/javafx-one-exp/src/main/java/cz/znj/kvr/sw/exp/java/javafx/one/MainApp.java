package cz.znj.kvr.sw.exp.java.javafx.one;

import cz.znj.kvr.sw.exp.java.javafx.one.controller.PersonOverviewController;
import cz.znj.kvr.sw.exp.java.javafx.one.model.Person;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;


public class MainApp extends Application
{

	private Stage primaryStage;
	private BorderPane rootLayout;

	public 				MainApp()
	{
		// Add some sample data
		personData.add(new Person("Zbynek", "Vyskovsky"));
		personData.add(new Person("Karolina", "Svetla"));
		personData.add(new Person("Josef", "Nejedly"));
		personData.add(new Person("Alois", "Jirasek"));
		personData.add(new Person("Karel", "Ctvrty"));
	}

	@Override
	public void			start(Stage primaryStage)
	{
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("AddressApp");

		initRootLayout();

		showPersonOverview();
	}

	/**
	 * Initializes the root layout.
	 */
	public void			initRootLayout()
	{
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			// Show the scene containing the root layout.
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Shows the person overview inside the root layout.
	 */
	public void			showPersonOverview()
	{
		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/PersonOverview.fxml"));
			AnchorPane personOverview = (AnchorPane) loader.load();

			// Set person overview into the center of root layout.
			rootLayout.setCenter(personOverview);

			// Give the controller access to the main app.
			PersonOverviewController controller = loader.getController();
			controller.setMainApp(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the main stage.
	 * @return
	 */
	public Stage			getPrimaryStage()
	{
		return primaryStage;
	}

	/**
	 * Returns the data as an observable list of Persons.
	 * @return
	 */
	public ObservableList<Person>	getPersonData() {
		return personData;
	}

	/**
	 * The data as an observable list of Persons.
	 */
	private ObservableList<Person>	personData = FXCollections.observableArrayList();
}
