package eu.europa.ec.digit.apigw.publisher.controller;

import eu.europa.ec.digit.apigw.publisher.entity.AliasInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("/certificate")
@Api(value = "Certificate Publisher", tags = {"Certificate Publisher"}, description = "If you want the API Gateway to trust your backend SSL endpoint, you need to add your public key to the truststore")
@Slf4j
public class CertificateController {

    @Value("${trust.certificate.path}")
    private String trustCertificatePath;

    @Value("${trust.certificate.password}")
    private String trustCertificatePassword;

    @Value("${ssl.profile.manager}")
    private String sslProfileManager;

    @ApiOperation(value = "Get all certificates")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "All certificates")
    })
    @GetMapping
    public ResponseEntity<List<AliasInfo>> getAll() {
        List<AliasInfo> theAlias = new ArrayList<>();
        try {
            FileInputStream is = new FileInputStream(trustCertificatePath);

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, trustCertificatePassword.toCharArray());
            Enumeration<String> aliases = keystore.aliases();
            while(aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                AliasInfo aliasInfo = new AliasInfo();
                aliasInfo.setAlias(alias);
                X509Certificate certificate = (X509Certificate) keystore.getCertificate(alias);
                aliasInfo.setIssuerDN(certificate.getIssuerDN().getName());
                aliasInfo.setSubjectDN(certificate.getSubjectDN().getName());
                theAlias.add(aliasInfo);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(theAlias, HttpStatus.OK);
    }

    @ApiOperation(value = "Get certificate by alias")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Certificate")
    })
    @GetMapping(path = "/{alias}")
    public ResponseEntity<AliasInfo> getAlias(@PathVariable String alias) {
        AliasInfo aliasInfo = new AliasInfo();
        HttpStatus resultStatusCode = HttpStatus.NOT_FOUND;
        try {
            FileInputStream is = new FileInputStream(trustCertificatePath);

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, trustCertificatePassword.toCharArray());
            Enumeration<String> aliases = keystore.aliases();
            while(aliases.hasMoreElements()) {
                String insertedAlias = aliases.nextElement();
                if(insertedAlias.equals(alias)) {
                    X509Certificate certificate = (X509Certificate) keystore.getCertificate(alias);
                    aliasInfo.setAlias(insertedAlias);
                    aliasInfo.setIssuerDN(certificate.getIssuerDN().getName());
                    aliasInfo.setSubjectDN(certificate.getSubjectDN().getName());
                    resultStatusCode = HttpStatus.OK;
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(aliasInfo, resultStatusCode);
    }

    @ApiOperation(value = "Install a certificate")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Certificate installed")
    })
    @PostMapping(path = "/{alias}")
    public ResponseEntity<AliasInfo> certificateUpload(@PathVariable String alias, @RequestParam("file") MultipartFile file) {
        AliasInfo aliasInfo = new AliasInfo();
        try {
            FileInputStream is = new FileInputStream(trustCertificatePath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, trustCertificatePassword.toCharArray());

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate newTrusted = certificateFactory.generateCertificate(file.getInputStream());

            X509Certificate x509Object = (X509Certificate) newTrusted;
            aliasInfo.setSubjectDN(x509Object.getSubjectDN().getName());
            aliasInfo.setIssuerDN(x509Object.getIssuerDN().getName());
            aliasInfo.setAlias(alias);

            keystore.setCertificateEntry(alias, newTrusted);

            FileOutputStream storeOutputStream = new FileOutputStream(trustCertificatePath);
            keystore.store(storeOutputStream, trustCertificatePassword.toCharArray());

            File profileXml = new File(sslProfileManager);
            profileXml.setLastModified(Calendar.getInstance().getTimeInMillis());

        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }
        return new ResponseEntity<>(aliasInfo, HttpStatus.OK);
    }

    @ApiOperation(value = "Remove a certificate")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Certificate removed")
    })
    @DeleteMapping(path = "/{alias}")
    public ResponseEntity<AliasInfo> removeFromTrust(@PathVariable String alias) {
        AliasInfo aliasInfo = new AliasInfo();
        try {
            FileInputStream is = new FileInputStream(trustCertificatePath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, trustCertificatePassword.toCharArray());

            keystore.deleteEntry(alias);

            FileOutputStream storeOutputStream = new FileOutputStream(trustCertificatePath);
            keystore.store(storeOutputStream, trustCertificatePassword.toCharArray());

            File profileXml = new File(sslProfileManager);
            profileXml.setLastModified(Calendar.getInstance().getTimeInMillis());

        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(aliasInfo, HttpStatus.OK);
    }
}
