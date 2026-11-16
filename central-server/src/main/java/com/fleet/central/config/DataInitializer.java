package com.fleet.central.config;

import com.fleet.central.domain.*;
import com.fleet.central.repository.ChargingStationRepository;
import com.fleet.central.repository.ScooterRepository;
import com.fleet.central.repository.UserRepository;
import com.fleet.central.repository.ZoneRepository;
import com.fleet.central.service.GeoSpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ChargingStationRepository stationRepository;
    private final ScooterRepository scooterRepository;
    private final GeoSpatialService geoSpatialService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ZoneRepository zoneRepository,
                           ChargingStationRepository stationRepository,
                           ScooterRepository scooterRepository,
                           GeoSpatialService geoSpatialService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.stationRepository = stationRepository;
        this.scooterRepository = scooterRepository;
        this.geoSpatialService = geoSpatialService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting Central Server data initialization & Redis sync...");

        // 1. Seed Users if not present
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User(
                    "usr_admin_01",
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ROLE_ADMIN",
                    "Fleet Administrator"
            ));
            log.info("Seeded default admin user: 'admin'");
        }

        if (!userRepository.existsByUsername("rider")) {
            userRepository.save(new User(
                    "usr_rider_01",
                    "rider",
                    passwordEncoder.encode("rider123"),
                    "ROLE_USER",
                    "Standard Rider"
            ));
            log.info("Seeded default rider user: 'rider'");
        }

        // 2. Seed Zones if not present
        if (zoneRepository.count() == 0) {
            zoneRepository.saveAll(List.of(
                    new Zone("zone_centru", "Downtown & Historic Center", 5, 20),
                    new Zone("zone_campus", "University & Student Campus", 5, 15),
                    new Zone("zone_bega", "Bega River Green Corridor", 3, 10)
            ));
            log.info("Seeded operational zones.");
        }

        // 3. Seed Charging Stations if not present
        if (stationRepository.count() == 0) {
            stationRepository.saveAll(List.of(
                    new ChargingStation("station_a", "Victory Square Station", 45.7538, 21.2257, 15, 8084, "station_a_cnt", StationStatus.ACTIVE),
                    new ChargingStation("station_b", "Central Park Station", 45.7508, 21.2211, 15, 8085, "station_b_cnt", StationStatus.ACTIVE),
                    new ChargingStation("station_c", "Union Square Station", 45.7576, 21.2288, 20, 8086, "station_c_cnt", StationStatus.ACTIVE),
                    new ChargingStation("station_d", "Bega Promenade Station", 45.7495, 21.2351, 15, 8087, "station_d_cnt", StationStatus.ACTIVE)
            ));
            log.info("Seeded charging stations.");
        }

        // 4. Seed Scooters if not present
        if (scooterRepository.count() == 0) {
            ChargingStation sta = stationRepository.findById("station_a").orElse(null);
            ChargingStation stb = stationRepository.findById("station_b").orElse(null);
            ChargingStation stc = stationRepository.findById("station_c").orElse(null);
            ChargingStation std = stationRepository.findById("station_d").orElse(null);
            Zone zc = zoneRepository.findById("zone_centru").orElse(null);
            Zone zb = zoneRepository.findById("zone_bega").orElse(null);

            scooterRepository.saveAll(List.of(
                    new Scooter("sc_101", "Ninebot Max G2", 92, ScooterStatus.AVAILABLE, 45.7540, 21.2259, sta, zc),
                    new Scooter("sc_102", "Ninebot Max G2", 84, ScooterStatus.AVAILABLE, 45.7534, 21.2252, sta, zc),
                    new Scooter("sc_103", "Xiaomi Pro 4", 77, ScooterStatus.AVAILABLE, 45.7544, 21.2266, sta, zc),
                    new Scooter("sc_104", "Segway Urban E+", 96, ScooterStatus.AVAILABLE, 45.7528, 21.2248, sta, zc),
                    new Scooter("sc_201", "Ninebot Max G2", 88, ScooterStatus.AVAILABLE, 45.7508, 21.2211, stb, zc),
                    new Scooter("sc_202", "Xiaomi Pro 4", 65, ScooterStatus.AVAILABLE, 45.7502, 21.2202, stb, zc),
                    new Scooter("sc_301", "Segway Urban E+", 91, ScooterStatus.AVAILABLE, 45.7576, 21.2288, stc, zc),
                    new Scooter("sc_302", "Ninebot Max G2", 79, ScooterStatus.AVAILABLE, 45.7583, 21.2295, stc, zc),
                    new Scooter("sc_303", "Xiaomi Pro 4", 54, ScooterStatus.AVAILABLE, 45.7573, 21.2282, stc, zc),
                    new Scooter("sc_401", "Ninebot Max G2", 95, ScooterStatus.AVAILABLE, 45.7495, 21.2351, std, zb),
                    new Scooter("sc_402", "Xiaomi Pro 4", 81, ScooterStatus.AVAILABLE, 45.7488, 21.2361, std, zb)
            ));
            log.info("Seeded initial scooters.");
        }

        // 5. Re-index all AVAILABLE stationary scooters into Redis GEO
        List<Scooter> available = scooterRepository.findByCurrentStatus(ScooterStatus.AVAILABLE);
        for (Scooter s : available) {
            geoSpatialService.indexScooter(s);
        }
        log.info("Synchronized {} stationary scooters into Redis GEO.", available.size());
    }
}
