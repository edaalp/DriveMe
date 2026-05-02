package com.driveme.backend.config;

import com.driveme.backend.common.TransmissionType;
import com.driveme.backend.common.VehicleDocumentType;
import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.entity.Admin;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.entity.VehicleDocument;
import com.driveme.backend.repository.AdminRepository;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Seeds fake pending drivers and vehicles for admin panel development/testing.
 * Only runs when the "dev" profile is active.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final VehicleRepository vehicleRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedPassengersAndVehicles();
        seedDrivers();
        log.info("Dev seed data loaded successfully");
    }

    private void seedAdmin() {
        String email = "admin@driveme.com";
        String plainPassword = "admin123";
        String hashedPassword = passwordEncoder.encode(plainPassword);

        if (adminRepository.existsByEmail(email)) {
            // Update existing admin's password to ensure consistent dev credentials
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            Admin admin = adminRepository.findByEmail(email).get();
            admin.setPasswordHash(hashedPassword);
            admin.setFullName("Admin User");
            admin.setActive(true);
            adminRepository.save(admin);
            log.info("Updated seed admin user: admin@driveme.com (password: {})", plainPassword);
            return;
        }

        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setFullName("Admin User");
        admin.setUserName("admin");
        admin.setPasswordHash(hashedPassword);
        admin.setActive(true);
        adminRepository.save(admin);

        log.info("Seeded admin user: admin@driveme.com (password: {})", plainPassword);
    }

    private void seedPassengersAndVehicles() {
        if (vehicleRepository.existsByPlateNumber("34 ABC 123")) {
            log.info("Seed vehicles already exist, skipping");
            return;
        }

        String hashedPassword = passwordEncoder.encode("test123");

        // Passenger 1 - PENDING
        Passenger p1 = new Passenger();
        p1.setEmail("ayse.yilmaz@example.com");
        p1.setFullName("Ayse Yilmaz");
        p1.setUserName("ayseyilmaz");
        p1.setPhoneNumber("+905321234567");
        p1.setPasswordHash(hashedPassword);
        p1.setActive(true);
        p1.setVerificationStatus(VerificationStatus.PENDING);
        if (!passengerRepository.existsByEmail(p1.getEmail())) {
            p1 = passengerRepository.save(p1);
        } else {
            p1 = passengerRepository.findByEmail(p1.getEmail()).get();
        }

        // Passenger 2 - VERIFIED
        Passenger p2 = new Passenger();
        p2.setEmail("mehmet.kara@example.com");
        p2.setFullName("Mehmet Kara");
        p2.setUserName("mehmetkara");
        p2.setPhoneNumber("+905559876543");
        p2.setPasswordHash(hashedPassword);
        p2.setActive(true);
        p2.setVerificationStatus(VerificationStatus.VERIFIED);
        if (!passengerRepository.existsByEmail(p2.getEmail())) {
            p2 = passengerRepository.save(p2);
        } else {
            p2 = passengerRepository.findByEmail(p2.getEmail()).get();
        }

        // Passenger 3 - PENDING
        Passenger p3 = new Passenger();
        p3.setEmail("zeynep.demir@example.com");
        p3.setFullName("Zeynep Demir");
        p3.setUserName("zeynepdemir");
        p3.setPhoneNumber("+905441112233");
        p3.setPasswordHash(hashedPassword);
        p3.setActive(true);
        p3.setVerificationStatus(VerificationStatus.PENDING);
        if (!passengerRepository.existsByEmail(p3.getEmail())) {
            p3 = passengerRepository.save(p3);
        } else {
            p3 = passengerRepository.findByEmail(p3.getEmail()).get();
        }

        // Passenger 4 - REJECTED
        Passenger p4 = new Passenger();
        p4.setEmail("esra.gokcinar@example.com");
        p4.setFullName("Esra Gokcinar");
        p4.setUserName("esragokcinar");
        p4.setPhoneNumber("+905953021287");
        p4.setPasswordHash(hashedPassword);
        p4.setActive(true);
        p4.setVerificationStatus(VerificationStatus.REJECTED);
        p4.setRejectionReason("Document verification failed - invalid TC number");
        if (!passengerRepository.existsByEmail(p4.getEmail())) {
            p4 = passengerRepository.save(p4);
        } else {
            p4 = passengerRepository.findByEmail(p4.getEmail()).get();
        }

        byte[] fakePdf = buildFakePdf("Vehicle Registration Certificate");

        // Vehicle 1 - PENDING with document
        Vehicle v1 = new Vehicle();
        v1.setPlateNumber("34 ABC 123");
        v1.setBrand("Toyota");
        v1.setModel("Corolla");
        v1.setYear(2022);
        v1.setTransmission(TransmissionType.AUTOMATIC);
        v1.setStatus(VerificationStatus.PENDING);
        v1.setPassenger(p1);
        attachVehicleDoc(v1, "toyota_corolla_ruhsat.pdf", VehicleDocumentType.RUHSAT_FRONT, fakePdf);
        vehicleRepository.save(v1);

        // Vehicle 2 - VERIFIED with document
        Vehicle v2 = new Vehicle();
        v2.setPlateNumber("06 DEF 456");
        v2.setBrand("Honda");
        v2.setModel("Civic");
        v2.setYear(2023);
        v2.setTransmission(TransmissionType.AUTOMATIC);
        v2.setStatus(VerificationStatus.VERIFIED);
        v2.setPassenger(p2);
        attachVehicleDoc(v2, "honda_civic_registration.pdf", VehicleDocumentType.RUHSAT_FRONT, fakePdf);
        vehicleRepository.save(v2);

        // Vehicle 3 - PENDING without document
        Vehicle v3 = new Vehicle();
        v3.setPlateNumber("35 GHI 789");
        v3.setBrand("Volkswagen");
        v3.setModel("Golf");
        v3.setYear(2021);
        v3.setTransmission(TransmissionType.MANUAL);
        v3.setStatus(VerificationStatus.PENDING);
        v3.setPassenger(p3);
        vehicleRepository.save(v3);

        // Vehicle 4 - VERIFIED
        Vehicle v4 = new Vehicle();
        v4.setPlateNumber("34 JKL 012");
        v4.setBrand("BMW");
        v4.setModel("320i");
        v4.setYear(2024);
        v4.setTransmission(TransmissionType.AUTOMATIC);
        v4.setStatus(VerificationStatus.VERIFIED);
        v4.setPassenger(p2);
        attachVehicleDoc(v4, "bmw_320i_ruhsat.pdf", VehicleDocumentType.RUHSAT_FRONT, fakePdf);
        vehicleRepository.save(v4);

        log.info("Seeded 4 passengers (1 pending, 1 verified, 1 pending, 1 rejected) and 4 vehicles");
    }

    private void seedDrivers() {
        if (driverRepository.existsByEmail("ali.ozturk@example.com")) {
            log.info("Seed drivers already exist, skipping");
            return;
        }

        String hashedPassword = passwordEncoder.encode("test123");
        byte[] fakeCriminalRecord = buildFakePdf("Criminal Record Certificate - No Record Found");

        // Driver 1 - PENDING with criminal record
        Driver d1 = new Driver();
        d1.setEmail("ali.ozturk@example.com");
        d1.setFullName("Ali Ozturk");
        d1.setUserName("aliozturk");
        d1.setPhoneNumber("+905331234567");
        d1.setPasswordHash(hashedPassword);
        d1.setActive(true);
        d1.setLicenseNumber("ANK-2020-12345");
        d1.setDriverLicenseNumber("B-987654");
        d1.setTckNo(12345678901L);
        d1.setLicenseIssueDate(new Date(1577836800000L)); // 2020-01-01
        d1.setAvailable(true);
        d1.setMaxPickupRadiusKm(10.0);
        d1.setMaxDropoffRadiusKm(50.0);
        d1.setAcceptsPets(true);
        d1.setAvgRating(0.0);
        d1.setVerificationStatus(VerificationStatus.PENDING);
        d1.setCriminalRecordFile(fakeCriminalRecord);
        d1.setCriminalRecordFileName("ali_ozturk_sabika_kaydi.pdf");
        driverRepository.save(d1);

        // Driver 2 - PENDING with criminal record
        Driver d2 = new Driver();
        d2.setEmail("fatma.celik@example.com");
        d2.setFullName("Fatma Celik");
        d2.setUserName("fatmacelik");
        d2.setPhoneNumber("+905449876543");
        d2.setPasswordHash(hashedPassword);
        d2.setActive(true);
        d2.setLicenseNumber("IST-2019-67890");
        d2.setDriverLicenseNumber("B-123456");
        d2.setTckNo(98765432109L);
        d2.setLicenseIssueDate(new Date(1546300800000L)); // 2019-01-01
        d2.setAvailable(false);
        d2.setMaxPickupRadiusKm(5.0);
        d2.setMaxDropoffRadiusKm(30.0);
        d2.setAcceptsPets(false);
        d2.setAvgRating(0.0);
        d2.setVerificationStatus(VerificationStatus.PENDING);
        d2.setCriminalRecordFile(fakeCriminalRecord);
        d2.setCriminalRecordFileName("fatma_celik_criminal_record.pdf");
        driverRepository.save(d2);

        // Driver 3 - PENDING without documents
        Driver d3 = new Driver();
        d3.setEmail("emre.sahin@example.com");
        d3.setFullName("Emre Sahin");
        d3.setUserName("emresahin");
        d3.setPhoneNumber("+905557778899");
        d3.setPasswordHash(hashedPassword);
        d3.setActive(true);
        d3.setLicenseNumber("IZM-2021-11111");
        d3.setDriverLicenseNumber("B-555666");
        d3.setTckNo(11223344556L);
        d3.setLicenseIssueDate(new Date(1609459200000L)); // 2021-01-01
        d3.setAvailable(true);
        d3.setMaxPickupRadiusKm(8.0);
        d3.setMaxDropoffRadiusKm(40.0);
        d3.setAcceptsPets(true);
        d3.setAvgRating(0.0);
        d3.setVerificationStatus(VerificationStatus.PENDING);
        driverRepository.save(d3);

        // Driver 4 - VERIFIED
        Driver d4 = new Driver();
        d4.setEmail("can.arslan@example.com");
        d4.setFullName("Can Arslan");
        d4.setUserName("canarslan");
        d4.setPhoneNumber("+905362223344");
        d4.setPasswordHash(hashedPassword);
        d4.setActive(true);
        d4.setLicenseNumber("IST-2018-99999");
        d4.setDriverLicenseNumber("B-111222");
        d4.setTckNo(99887766554L);
        d4.setLicenseIssueDate(new Date(1514764800000L)); // 2018-01-01
        d4.setAvailable(true);
        d4.setMaxPickupRadiusKm(15.0);
        d4.setMaxDropoffRadiusKm(60.0);
        d4.setAcceptsPets(false);
        d4.setAvgRating(4.7);
        d4.setVerificationStatus(VerificationStatus.VERIFIED);
        d4.setCriminalRecordFile(fakeCriminalRecord);
        d4.setCriminalRecordFileName("can_arslan_sabika.pdf");
        driverRepository.save(d4);

        log.info("Seeded 4 drivers (3 pending, 1 verified)");
    }

    private static void attachVehicleDoc(
            Vehicle vehicle,
            String fileName,
            VehicleDocumentType type,
            byte[] content) {
        VehicleDocument d = new VehicleDocument();
        d.setVehicle(vehicle);
        d.setFileName(fileName);
        d.setDocumentType(type);
        d.setFileContent(content);
        vehicle.getDocuments().add(d);
    }

    /**
     * Builds a minimal valid PDF with the given text content.
     */
    private byte[] buildFakePdf(String text) {
        String pdf = "%PDF-1.4\n" +
                "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n" +
                "5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n" +
                "4 0 obj<</Length " + (44 + text.length()) + ">>\nstream\nBT /F1 16 Tf 50 700 Td (" + text + ") Tj ET\nendstream\nendobj\n" +
                "xref\n0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000058 00000 n \n" +
                "0000000115 00000 n \n" +
                "0000000266 00000 n \n" +
                "0000000206 00000 n \n" +
                "trailer<</Size 6/Root 1 0 R>>\nstartxref\n370\n%%EOF";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}
