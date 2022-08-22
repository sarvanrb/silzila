package org.silzila.app.security.service;

import org.silzila.app.repository.DBConnectionRepository;
import org.silzila.app.security.encryption.AES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.silzila.app.dto.DBConnectionDTO;
import org.silzila.app.model.DBConnection;
import org.silzila.app.payload.request.DBConnectionRequest;
import org.silzila.app.exception.RecordNotFoundException;
import org.silzila.app.exception.BadRequestException;
import org.silzila.app.security.encryption.AES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;

@Service
public class DBConnectionService {

    @Autowired
    DBConnectionRepository dbConnectionRepository;

    ModelMapper mapper = new ModelMapper();

    @Value("${passwordEncryptionSecretKey}")
    private String passwordEncryptionSecretKey;

    public List<DBConnectionDTO> getAllDBConnections(String userId) {
        // fetch all DB connections for the user
        List<DBConnection> dbConnections = dbConnectionRepository.findByUserId(userId);
        // convert to DTO object to not show Password
        List<DBConnectionDTO> dtos = new ArrayList<>();
        dbConnections.forEach(dbconnection -> dtos.add(mapper.map(dbconnection,
                DBConnectionDTO.class)));
        return dtos;
    }

    public DBConnectionDTO getDBConnectionById(String id, String userId)
            throws RecordNotFoundException {
        // fetch the particular DB connection for the user
        Optional<DBConnection> optionalDBConnection = dbConnectionRepository.findByIdAndUserId(id, userId);
        // if no connection details, then send NOT FOUND Error
        if (!optionalDBConnection.isPresent()) {
            throw new RecordNotFoundException("Error: No such Connection Id exists");
        }
        DBConnection dbConnection = optionalDBConnection.get();
        String decryptedPassword = AES.decrypt(dbConnection.getPassword(), passwordEncryptionSecretKey);
        System.out.println(" ========== password = " + dbConnection.getPassword() + " decrypted password = "
                + decryptedPassword);
        // convert to DTO object to not show Password
        DBConnectionDTO dto = mapper.map(dbConnection, DBConnectionDTO.class);
        return dto;
    }

    public DBConnectionDTO createDBConnection(DBConnectionRequest dbConnectionRequest, String userId)
            throws BadRequestException {
        // check if connection name is alredy used for the requester
        List<DBConnection> connection_list = dbConnectionRepository.findByUserIdAndConnectionName(userId,
                dbConnectionRequest.getConnectionName());
        // if connection name is alredy used, send error
        if (!connection_list.isEmpty()) {
            throw new BadRequestException("Error: Connection Name is already taken!");
        }
        String encryptedPassword = AES.encrypt(dbConnectionRequest.getPassword(), passwordEncryptionSecretKey);
        System.out.println(" ========== password = " + dbConnectionRequest.getPassword() + " encrypted password = "
                + encryptedPassword);
        // create DB Connection object and save it to DB
        DBConnection dbConnection = new DBConnection(
                userId,
                dbConnectionRequest.getVendor(),
                dbConnectionRequest.getServer(),
                dbConnectionRequest.getPort(),
                dbConnectionRequest.getDatabase(),
                dbConnectionRequest.getUsername(),
                encryptedPassword, // dbConnectionRequest.getPassword(),
                dbConnectionRequest.getConnectionName());
        dbConnectionRepository.save(dbConnection);
        DBConnectionDTO dto = mapper.map(dbConnection, DBConnectionDTO.class);
        return dto;
    }

    public DBConnectionDTO updateDBConnection(String id, DBConnectionRequest dbConnectionRequest, String userId)
            throws RecordNotFoundException, BadRequestException {
        // fetch the particular DB connection for the user
        Optional<DBConnection> optionalDBConnection = dbConnectionRepository.findByIdAndUserId(id, userId);
        // if no connection details, then send NOT FOUND Error
        if (!optionalDBConnection.isPresent()) {
            throw new RecordNotFoundException("Error: No such Connection Id exists");
        }
        List<DBConnection> dbConnections = dbConnectionRepository
                .findByIdNotAndUserIdAndConnectionName(
                        id, userId, dbConnectionRequest.getConnectionName());
        if (!dbConnections.isEmpty()) {
            throw new BadRequestException("Error: Connection Name is alread taken!");
        }
        DBConnection _dbConnection = optionalDBConnection.get();
        _dbConnection.setConnectionName(dbConnectionRequest.getConnectionName());
        _dbConnection.setVendor(dbConnectionRequest.getVendor());
        _dbConnection.setServer(dbConnectionRequest.getServer());
        _dbConnection.setPort(dbConnectionRequest.getPort());
        _dbConnection.setDatabase(dbConnectionRequest.getDatabase());
        _dbConnection.setUsername(dbConnectionRequest.getUsername());
        _dbConnection.setPassword(dbConnectionRequest.getDatabase());
        dbConnectionRepository.save(_dbConnection);
        DBConnectionDTO dto = mapper.map(_dbConnection, DBConnectionDTO.class);
        return dto;
    }

    public void deleteDBConnection(String id, String userId)
            throws RecordNotFoundException {
        // fetch the particular DB connection for the user
        Optional<DBConnection> optionalDBConnection = dbConnectionRepository.findByIdAndUserId(id, userId);
        // if no connection details, then send NOT FOUND Error
        if (!optionalDBConnection.isPresent()) {
            throw new RecordNotFoundException("Error: No such Connection Id exists");
        }
        // delete the record from DB
        dbConnectionRepository.deleteById(id);
    }

}
