package univ.lorraine.simpleChat.SimpleChat.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import univ.lorraine.simpleChat.SimpleChat.fileUpload.FileStorageService;
import univ.lorraine.simpleChat.SimpleChat.model.File;
import univ.lorraine.simpleChat.SimpleChat.model.Groupe;
import univ.lorraine.simpleChat.SimpleChat.model.Message;
import univ.lorraine.simpleChat.SimpleChat.model.User;
import univ.lorraine.simpleChat.SimpleChat.modelTemplate.MessageTemplate;
import univ.lorraine.simpleChat.SimpleChat.service.GroupeService;
import univ.lorraine.simpleChat.SimpleChat.service.UserService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/api/fileUpload")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Api(value = "Simple Chat")
@RestController
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final MessageController messageController;

    @Autowired
    private FileStorageService fileStorageService;
    private UserService userService;
    private GroupeService groupeService;

    public FileController(MessageController messageController, UserService userService, GroupeService groupeService) {
        this.messageController = messageController;
        this.userService = userService;
        this.groupeService = groupeService;
    }

    @ApiOperation("Sauvegarde d'un fichier en BDD puis envoie d'un message avec l'id du fichier et le nom du fichier")
    @PostMapping("/uploadFile")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId, @RequestParam("groupeId") Long groupeId) {
        File fileToGet= fileStorageService.storeFile(file);

        Groupe groupe = this.groupeService.find(groupeId);

        if(groupe == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le groupe d'id " + groupeId + " n'a pas été trouvé. Nous ne pouvons pas créer un sondage sans l'associer à un groupe.");
        }

        User user = this.userService.find(userId);

        if(user == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("L'utilisateur d'id " + userId + " n'a pas été trouvé. Nous ne pouvons pas créer un sondage sans initiateur.");
        }


        this.sendFileMessage(fileToGet, user, groupe);

//        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("/downloadFile/")
//                .path(fileName)
//                .toUriString();
//
//        return new UploadFileResponse(fileName, fileDownloadUri,
//                file.getContentType(), file.getSize());
        return ResponseEntity.ok()
                .body(toJSON(fileToGet));

    }

    @ApiOperation("Envoi d'un messagetemplate avec l'id du fichier et le nom du fichier")
    private void sendFileMessage(File file, User user, Groupe groupe){
        MessageTemplate messageTemplate = new MessageTemplate();
        messageTemplate.setGroupId(groupe.getId());
        messageTemplate.setUserId(user.getId());
        messageTemplate.setContenu(file.getId() + ":" + file.getName());
        messageTemplate.setType("fichier");

        this.messageController.sendMessage(messageTemplate);

    }

    @ApiOperation("Sauvegarde de plusieurs fichiers en BDD puis envoie d'un message par fichier avec l'id du fichier et le nom du fichier")
    @PostMapping("/uploadMultipleFiles")
    public ResponseEntity<List> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files, @RequestParam("userId") Long userId, @RequestParam("groupeId") Long groupeId) {
        return ResponseEntity.ok(Arrays.asList(files).stream().map(file -> uploadFile(file, userId, groupeId).getBody()).collect(Collectors.toList()));
    }

    @ApiOperation("Récupère le fichier via son id")
    @GetMapping("/getFile/{fileId}")
    public ResponseEntity<Resource> getFile(@PathVariable Long fileId, HttpServletRequest request) {
        File fileToGet = fileStorageService.getFileById(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileToGet.getContentType()))
                .body(new ByteArrayResource(fileToGet.getData()));
    }

    @ApiOperation("Récupère les informations d'un fichier via son id")
    @GetMapping("/getFileData/{fileId}")
    public ResponseEntity<String> getFileData(@PathVariable Long fileId, HttpServletRequest request) {

        File fileToGet = fileStorageService.getFileById(fileId);

        return ResponseEntity.ok()
                .body(toJSON(fileToGet));
    }

    @ApiOperation("Téléchargement d'un fichier via son id")
    @GetMapping("/downloadFile/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, HttpServletRequest request) {

        File fileToGet = fileStorageService.getFileById(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileToGet.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileToGet.getName()+ "\"")
                .body(new ByteArrayResource(fileToGet.getData()));
    }

    @ApiOperation("Retourne un JSON avec les informations d'un fichier en paramètre")
    public String toJSON(File file)
    {
        JsonObject json = Json.createObjectBuilder()
                .add("fileId", file.getId())
                .add("fileName", file.getName())
                .add("fileType", file.getContentType())
                .add("fileSize", file.getData().length)
                .build();
        return json.toString();
    }
}

