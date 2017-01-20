package parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class AsciiToCsv {

	public static void main(String args[]) throws IOException {

		String file_to_parse = "ch4y2001m5";
		ArrayList<String> lines = new ArrayList<String>();

		BufferedReader f = new BufferedReader(new FileReader("Original_ASCII_files\\" + file_to_parse + ".txt"));

		Scanner scanner;
		try {
			PrintWriter output = new PrintWriter(
					new BufferedWriter(new FileWriter("Parsed_CSVs\\" + file_to_parse + ".csv")));
			output.println("latitude,longitude,value");
			scanner = new Scanner(f);

			String headers = scanner.nextLine();
			Scanner scanheaders = new Scanner(headers);
			scanheaders.next();
			double ncols = scanheaders.nextDouble();

			headers = scanner.nextLine();
			headers = scanner.nextLine();
			scanheaders = new Scanner(headers);
			scanheaders.next();
			double nrows = scanheaders.nextDouble();

			headers = scanner.nextLine();
			headers = scanner.nextLine();
			scanheaders = new Scanner(headers);
			scanheaders.next();
			double xllcorner = scanheaders.nextDouble();

			headers = scanner.nextLine();
			headers = scanner.nextLine();
			scanheaders = new Scanner(headers);
			scanheaders.next();
			double yllcorner = scanheaders.nextDouble();

			headers = scanner.nextLine();
			headers = scanner.nextLine();
			scanheaders = new Scanner(headers);
			scanheaders.next();
			double cellsize = scanheaders.nextDouble();

			headers = scanner.nextLine();
			headers = scanner.nextLine();
			scanheaders = new Scanner(headers);
			scanheaders.next();
			double NODATA_value = scanheaders.nextDouble();
			scanheaders.close();

			System.out.println(ncols + " ");
			int counter = 0;
			double longitude = xllcorner;
			double latitude = yllcorner + (cellsize * (nrows - 1));
			int rows = 0;
			int columns = 0;
			double max = 2017;
			double min = 2017;

			while (scanner.hasNextLine()) {
				String scanning = scanner.nextLine();
				Scanner linescan = new Scanner(scanning);
				while (linescan.hasNext()) {
					double value = linescan.nextDouble();
					if (value != NODATA_value) {
						if (max == 2017 && min == 2017) {
							max = value;
							min = value;
							lines.add((latitude - rows * cellsize) + "," + (longitude + columns * cellsize) + ","
									+ value);
						} else if (value > max) {
							max = value;
							lines.add(1, (latitude - rows * cellsize) + "," + (longitude + columns * cellsize) + ","
									+ value);
						} else if (value < min) {
							min = value;
							lines.add(0, (latitude - rows * cellsize) + "," + (longitude + columns * cellsize) + ","
									+ value);
							if (lines.size() > 2) {
								String temp = lines.remove(2);
								lines.add(1, temp);
							}
						} else {
							lines.add((latitude - rows * cellsize) + "," + (longitude + columns * cellsize) + ","
									+ value);
						}

					}
					columns++;
					if (columns % 1404 == 0) {
						if (rows < 923) {
							columns = 0;
						}
						rows++;
					}
					counter++;
				}
				linescan.close();
			}

			while (!lines.isEmpty()) {
				output.println(lines.remove(0));
			}

			// Print out Max and Min (TESTING PURPOSES)
			// System.out.println(max + " " + min);

			output.close();
			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}