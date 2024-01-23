package com.mannit.chatbot.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.mannit.chatbot.model.CurrentPatient;
import com.mannit.chatbot.model.Noappointment;
import com.mannit.chatbot.model.QueriedPatient;
import com.mannit.chatbot.repository.Currentpatientsrepo;
import com.mannit.chatbot.repository.Noappointmentrepo;
import com.mannit.chatbot.repository.QuriedpRepo;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

@RestController
public class SheetsQuickstart {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private final static Logger logger = LoggerFactory.getLogger(SheetsQuickstart.class);
    @Autowired
    private Currentpatientsrepo repo;
    @Autowired
    private Noappointmentrepo no_app_repo;
    @Autowired
    private QuriedpRepo quried_repo;

	
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	private String lastProcessedTimestampYesPatients = "";
	private String lastProcessedTimestampNoAppointment = "";
	private String lastProcessedTimestampCallBack = "";

	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		logger.info("<in the getcredentials method>");
		InputStream in = SheetsQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
				.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	@Scheduled(fixedRate = 600000)
	public void getsheetdata() throws GeneralSecurityException, IOException {
		logger.info("<In the getSheetdata() method>");
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		final String spreadsheetId = "16mQkUXa6PeeH96WDFeATHPLFx_dYUUOGcXqJ9clkBek";
		final String range_1 = "Yes-patients";
		final String range_2 = "No-appointment";
		final String range_3 = "Call-me-back";
		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		logger.info("<Started Reading the spreadsheet with spreadsheet id >" + spreadsheetId);
		ValueRange response_1 = service.spreadsheets().values().get(spreadsheetId, range_1).execute();
		List<List<Object>> values1 = response_1.getValues();
		lastProcessedTimestampYesPatients = processSheetData("Yes-patients", values1,
				lastProcessedTimestampYesPatients);

		ValueRange response_2 = service.spreadsheets().values().get(spreadsheetId, range_2).execute();
		List<List<Object>> values2 = response_2.getValues();
		lastProcessedTimestampNoAppointment = processSheetData("No-appointment", values2,
				lastProcessedTimestampNoAppointment);

		ValueRange response_3 = service.spreadsheets().values().get(spreadsheetId, range_3).execute();
		List<List<Object>> values3 = response_3.getValues();
		lastProcessedTimestampCallBack = processSheetData("Call-me-back", values3, lastProcessedTimestampCallBack);
	}

	private String processSheetData(String sheetName, List<List<Object>> values, String lastProcessedTimestamp) {
		boolean isFirstRow = true;
		if (values == null || values.isEmpty()) {
			System.out.println("No data found for " + sheetName);
		} else {
			for (List<Object> row : values) {
				if (isFirstRow) {
					isFirstRow = false;
					continue;
				}
				String timestamp = row.get(0).toString();
				if (timestamp.compareTo(lastProcessedTimestamp) >= 0) {
					if (sheetName.equals("Yes-patients")) {
						CurrentPatient cp = new CurrentPatient();
						cp.setTimestamp(row.get(0).toString());
						cp.setName(row.get(1).toString());
						cp.setPhone_number(row.get(2).toString());
						cp.setDoctor_choice(row.get(3).toString());
						repo.save(cp);
					} else if (sheetName.equals("No-appointment")) {
						Noappointment noAppointment = new Noappointment();
						noAppointment.setName(row.get(1).toString());
						noAppointment.setPhone_number(row.get(2).toString());
						noAppointment.setTimestamp(row.get(0).toString());
						no_app_repo.save(noAppointment);
					} else if (sheetName.equals("Call-me-back")) {
						QueriedPatient qp = new QueriedPatient();
						qp.setName(row.get(1).toString());
						qp.setPhone_number(row.get(2).toString());
						qp.setTimestamp(row.get(0).toString());
						quried_repo.save(qp);
					}

					lastProcessedTimestamp = timestamp;
				}
			}
		}
		return lastProcessedTimestamp;
	}
	@RequestMapping(value = "/api/getbydate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, List<?>> getByDate(@RequestParam("date") String date) {
		String formattedDate = convertDateFormat(date, "yyyy-MM-dd", "MM/dd/yyyy");
		List<CurrentPatient> cp = repo.findByDate(formattedDate);
		System.out.println(cp.toString());
		List<QueriedPatient> qp = quried_repo.findByDate(formattedDate);
		List<Noappointment> np = no_app_repo.findByDate(formattedDate);
		System.out.println(cp);
		   List<Noappointment> uniqueNp = removeDuplicates(np, Noappointment::getTimestamp);
		   List<CurrentPatient> uniqueCp = removeDuplicates(cp, CurrentPatient::getTimestamp);
		   List<QueriedPatient> uniqueQp = removeDuplicates(qp, QueriedPatient::getTimestamp);
		   getsortedlist(uniqueCp);
		Map<String, List<?>> result = new HashMap<>();
		result.put("currentPatients", uniqueCp);
		result.put("queriedPatients", uniqueQp);
		result.put("noAppointments", uniqueNp);
		return result;
	}

	@RequestMapping(value = "/getdata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, List<?>> getcurrentpatient(Model model) throws ParseException {
		LocalDateTime current_time= LocalDateTime.now();
		 String formattedDate = convertDateFormat(current_time.toString(), "yyyy-MM-dd",
				  "MM/dd/yyyy");
		 System.out.println("formated date------------s"+formattedDate);
		 List<CurrentPatient> cp= repo.findByDate(formattedDate);
		 List<QueriedPatient> qp= quried_repo.findByDate(formattedDate);
		 List<Noappointment>np =no_app_repo.findByDate(formattedDate);
		   List<Noappointment> uniqueNp = removeDuplicates(np, Noappointment::getTimestamp);
		   List<CurrentPatient> uniqueCp = removeDuplicates(cp, CurrentPatient::getTimestamp);
		   List<QueriedPatient> uniqueQp = removeDuplicates(qp, QueriedPatient::getTimestamp);
		   getsortedlist(uniqueCp);
		Map<String, List<?>> result = new HashMap<>();
		
		result.put("currentPatients", uniqueCp);
		result.put("queriedPatients", uniqueQp);
		result.put("noAppointments", uniqueNp);
		return result;
	} 
	@PostMapping("/generate")
	public ResponseEntity<byte[]> generatePdf(@RequestBody Map<String, List<Map<String, String>>> rowData) {
		System.out.println("the data to convert in to pdf" + rowData.toString());
		byte[] pdfBytes = generatePdf2(rowData);

		try {
			  Path downloadsDirectory = Path.of(System.getProperty("user.home"), "Downloads");
		        String baseFileName = "report";
		        String fileExtension = ".pdf";

		        int counter = 1;
		        Path pdfPath;
		        do {
		            String fileName = baseFileName + (counter > 1 ? "(" + counter + ")" : "") + fileExtension;
		            pdfPath = downloadsDirectory.resolve(fileName);
		            counter++;
		        } while (Files.exists(pdfPath));
			Files.write(pdfPath, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			System.out.println("PDF file saved to: " + pdfPath.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("attachment", "report.pdf");

		return ResponseEntity.ok().headers(headers).contentLength(pdfBytes.length).body(pdfBytes);
	}
	 

	    public byte[] generatePdf2(Map<String, List<Map<String, String>>> rowData) {
	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

	        try (PdfWriter pdfWriter = new PdfWriter(byteArrayOutputStream);
	             PdfDocument pdfDocument = new PdfDocument(pdfWriter);
	             Document document = new Document(pdfDocument)) {

	            for (Map.Entry<String, List<Map<String, String>>> entry : rowData.entrySet()) {
	                String tableName = entry.getKey();
	                List<Map<String, String>> tableData = entry.getValue();
           if(!tableData.isEmpty()) {
        	   document.add(new Paragraph(tableName));
               Table table = createTable(tableData);
               document.add(table);
               document.add(new Paragraph("\n"));
        	   
           } else {
        	   document.add(new Paragraph("no data available for this table"));
           }
	        }
           document.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return byteArrayOutputStream.toByteArray();
	    }

	    private Table createTable(List<Map<String, String>> tableData) {
	        Table table = new Table(tableData.get(0).size());
	        float columnWidth = 200f;

	        for (Map.Entry<String, String> header : tableData.get(0).entrySet()) {
	            Cell headerCell = new Cell().add(new Paragraph(header.getKey()));
	            headerCell.setWidth(columnWidth);
	            table.addHeaderCell(headerCell);
	        }

	        for (Map<String, String> rowData : tableData) {
	            for (String value : rowData.values()) {
	                Cell cell = new Cell().add(new Paragraph(value));
	                cell.setWidth(columnWidth);
	                table.addCell(cell);
	            }
	        }
	        return table;
	    }
	    private static <T> List<T> removeDuplicates(List<T> list, java.util.function.Function<T, String> timestampExtractor) {
	        return list.stream()
	                .collect(Collectors.toMap(timestampExtractor, item -> item, (existing, replacement) -> existing))
	                .values().stream()
	                .collect(Collectors.toList());
	    }
	 public String convertDateFormat(String inputDateStr, String inputFormat, String outputFormat) {
        DateFormat inputDateFormat = new SimpleDateFormat(inputFormat);
        DateFormat outputDateFormat = new SimpleDateFormat(outputFormat);
        try {
            Date date = inputDateFormat.parse(inputDateStr);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
      
    }
	 @GetMapping("/m")
	 public int compare(String sheetdate, String dbdate) {
		 String dateString = "01/09/2024 10:33:42";
	        String dateFormat = "MM/dd/yyyy HH:mm:ss";
          sheetdate="01/09/2024 10:35:42";
          dbdate="01/09/2024 10:33:42";
	        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
       //if sheet date is greater =-1
	   //if sheet date is less than db =1
	   //if sheet date equals dbdate =0     
	        try {
	            Date parsedDate = sdf.parse(sheetdate);
	            Date parseddbdate = sdf.parse(dbdate);
	            parseddbdate.compareTo(parsedDate);
	            int parsevalue=parseddbdate.compareTo(parsedDate);
	            System.out.println(parsevalue);
	            return parsevalue;
	        } catch (ParseException e) {
	            e.printStackTrace();
	            return 2;
	        }
	 }
	 public void getsortedlist(List<CurrentPatient>CpatientList) {  //,List<Noappointment>NpatientList,List<QueriedPatient>QpatientList
		 SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		 Comparator<CurrentPatient> timestampComparator = Comparator.comparing(patient -> {
	            try {
	                return dateFormat.parse(patient.getTimestamp());
	            } catch (ParseException e) {
	                e.printStackTrace();
	                return new Date(0); 
	            }
	        });
	        Collections.sort(CpatientList, timestampComparator);

	        for (CurrentPatient patient : CpatientList) {
	            System.out.println(patient);
	        }
	    }
	 
	 
	 
}
