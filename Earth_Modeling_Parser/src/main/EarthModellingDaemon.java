/**
 * Main server thread that wakes up to check if any ASCII files have been added.
 * @author Anish Kunduru
 */

package main;

import java.io.File;
import java.io.IOException;

import parser.AsciiToCsv;

public class EarthModellingDaemon {

	public static final String INPUT_DIRECTORY_LOCATION = "Original_ASCII_files\\";
	
	public static void main(String[] args) throws IOException, InterruptedException {
		File inputDir = new File(INPUT_DIRECTORY_LOCATION);

		while (true) {
			
			while (inputDir.list().length > 0) {
				// ASCII to CSV
				AsciiToCsv.main(null);
				//TO-DO: CSV to Excel
			}

			Thread.sleep(600000);
		}
	}
}
