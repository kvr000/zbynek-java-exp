package cz.znj.kvr.sw.exp.java.javafx.one;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;


public class Main extends Application
{

	private Stage primaryStage;
	private BorderPane rootLayout;

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
			loader.setLocation(Main.class.getResource("view/RootLayout.fxml"));
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
			loader.setLocation(Main.class.getResource("view/PersonOverview.fxml"));
			AnchorPane personOverview = (AnchorPane) loader.load();

			// Set person overview into the center of root layout.
			rootLayout.setCenter(personOverview);
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

	public static void 		main(String[] args)
	{
		launch(args);
	}
}
