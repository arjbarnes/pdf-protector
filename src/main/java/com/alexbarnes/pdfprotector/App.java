package com.alexbarnes.pdfprotector;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class App 
{
	private static final String HELP_OPTION_NAME = "help";
	private static final String GUI_OPTION_NAME = "gui";
	private static final String EXISTING_PASSWORD_OPTION_NAME = "existingpassword";
	private static final String NEW_PASSWORD_OPTION_NAME = "newpassword";
	private static final String OUTPUT_OPTION_NAME = "output";
	private static final String OVERWRITE_OPTION_NAME = "overwrite";
	private static final Options CL_OPTIONS = new Options();			
						
	private static void generateCLOptions()
	{		
		final Option helpOption = Option.builder("h")
			.required(false)
			.hasArg(false)
			.longOpt(HELP_OPTION_NAME)
			.desc("print this message")
			.build();
		CL_OPTIONS.addOption(helpOption);

		final Option guiOption = Option.builder("g")
			.required(false)
			.hasArg(false)
			.longOpt(GUI_OPTION_NAME)
			.desc("launches a graphical user interface")
			.build();
		CL_OPTIONS.addOption(guiOption);
		
		final Option oldPasswordOption = Option.builder("e")
			.required(false)
			.hasArg(true)
			.longOpt(EXISTING_PASSWORD_OPTION_NAME)
			.desc("password to be used for opening the input PDF document(s), if needed")
			.build();
		CL_OPTIONS.addOption(oldPasswordOption);

		final Option newPasswordOption = Option.builder("p")
			.required(false)
			.hasArg(true)
			.longOpt(NEW_PASSWORD_OPTION_NAME)
			.desc("password to be used to protect the output PDF document(s)")
			.build();
		CL_OPTIONS.addOption(newPasswordOption);

		final Option outputOption = Option.builder("o")
			.required(false)
			.hasArg(true)
			.longOpt(OUTPUT_OPTION_NAME)
			.desc("location for saving output PDF document(s)")
			.build();
		CL_OPTIONS.addOption(outputOption);
		
		final Option overwriteOption = Option.builder("f")
			.required(false)
			.hasArg(false)
			.longOpt(OVERWRITE_OPTION_NAME)
			.desc("overwrite any pre-existing PDF document(s) having the same name in the output location")
			.build();
		
		CL_OPTIONS.addOption(overwriteOption);
	}
	
	private static void printHelp()
	{
		HelpFormatter hf = new HelpFormatter();
		System.out.println();
		System.out.println("+---------------+");
		System.out.println("| pdf-protector |");
		System.out.println("+---------------+");
		System.out.println();
		System.out.println("A program for applying (and removing) password protection to PDF document(s).");
		System.out.println();
		hf.printHelp("pdf-protector [options] file1.pdf file2.pdf file3.pdf ...", CL_OPTIONS);
		System.out.println();
	}
	
	private static void launchGUI()
	{
		System.out.println("ToDo: Launch GUI.");
		System.exit(0);
	}
	
    public static void main(String[] args) 
    {
		try
		{
			CommandLineParser clp = new DefaultParser();
			CommandLine cl = null;
			generateCLOptions();
			
			cl = clp.parse(CL_OPTIONS, args);
			
			if(cl.hasOption(HELP_OPTION_NAME))
			{
				printHelp();
				System.exit(0);
			}
			
			if(cl.hasOption(GUI_OPTION_NAME))
			{
				launchGUI();
			}
			
			String existingPassword = "";
			if(cl.hasOption(EXISTING_PASSWORD_OPTION_NAME))
			{	
				existingPassword = cl.getOptionValue(EXISTING_PASSWORD_OPTION_NAME);
			}
				
			String newPassword = "";
			if(cl.hasOption(NEW_PASSWORD_OPTION_NAME))
			{	
				newPassword = cl.getOptionValue(NEW_PASSWORD_OPTION_NAME);
			}		
			
			boolean overwrite = cl.hasOption(OVERWRITE_OPTION_NAME);
			
			File outputFile;
			if(cl.hasOption(OUTPUT_OPTION_NAME))
			{
				outputFile = new File(cl.getOptionValue(OUTPUT_OPTION_NAME));							
			}
			else
			{
				outputFile = new File(".");
			}

			String[] inputFilenames = cl.getArgs();
			if(inputFilenames.length < 1)
			{
				System.out.println("Error: No PDF document(s) specified for input.  For help, use 'pdf-protector -h'.");
				System.exit(0);
			}
			boolean success = true;
			for(String inputFilename : inputFilenames)
			{
				File inputFile = new File(inputFilename);
				
				if(outputFile.isDirectory())
				{
					success = success && processPDF(inputFile, existingPassword, newPassword, new File(outputFile, inputFile.getName()), overwrite);
				}	
				else if(inputFilenames.length == 1)
				{
					success = success && processPDF(inputFile, existingPassword, newPassword, outputFile, overwrite);
				}
				else
				{
					System.out.println("Error: Multiple PDF documents specified for input, but output location is not a directory.  For help, use 'pdf-protector -h'.");
					System.exit(0);
				}
				if(!success)
				{
					System.exit(0);
				}
			}
		}
		catch(ParseException e)
		{
			System.out.println("Error: " + e.getMessage() + ".  For help, use 'pdf-protector -h'.");
			System.exit(0);
		}		
    }
	
	private static boolean processPDF(File inputFile, String existingPassword, String newPassword, File outputFile, boolean overwrite)
	{
		try
		{
			PDDocument doc = PDDocument.load(inputFile, existingPassword);			
			
			AccessPermission ap = new AccessPermission();
				
			StandardProtectionPolicy spp = new StandardProtectionPolicy("", newPassword, ap);
			spp.setEncryptionKeyLength(256);
			spp.setPreferAES(true);
			spp.setPermissions(ap);
			doc.protect(spp);
			
			if(!outputFile.exists() || overwrite)
			{
				doc.save(outputFile);			
				doc.close();
				return true;
			}
			else
			{
				System.out.println("Error processing " + inputFile.toString() + ": A file of the same name already exists in the output location and the overwrite flag (-f) was not set.  For help, use 'pdf-protector -h'.");
				return false;
			}
		}
		catch(InvalidPasswordException e)
		{
			System.out.println("Error processing " + inputFile.toString() + ": " + e.getMessage());
			return false;
		}
		catch(IOException e)
		{
			System.out.println("Error processing " + inputFile.toString() + ": " + e.getMessage());
			return false;
		}
	}
}