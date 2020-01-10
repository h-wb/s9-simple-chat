
package univ.lorraine.simpleChat.SimpleChat.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.lorraine.simpleChat.SimpleChat.model.Groupe;
import univ.lorraine.simpleChat.SimpleChat.model.User;
import univ.lorraine.simpleChat.SimpleChat.modelTemplate.MessageTemplate;
import univ.lorraine.simpleChat.SimpleChat.ocsf.AutorisationException;
import univ.lorraine.simpleChat.SimpleChat.ocsf.ClientRunnable;
import univ.lorraine.simpleChat.SimpleChat.model.Message;
import univ.lorraine.simpleChat.SimpleChat.service.GroupeService;
import univ.lorraine.simpleChat.SimpleChat.service.GroupeUserService;
import univ.lorraine.simpleChat.SimpleChat.service.MessageService;
import univ.lorraine.simpleChat.SimpleChat.service.UserService;
import java.util.HashMap;


@RestController
@RequestMapping("/api/message")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Api( value="Simple Chat")
public class MessageController {
    private final UserService userService;
    private final GroupeService groupeService;
    private final MessageService messageService;
    private final GroupeUserService groupeUserService;

    public HashMap<Long, ClientRunnable> clientPool = new HashMap<>();

    public MessageController(UserService userService, GroupeService groupeService, MessageService messageService, GroupeUserService groupeUserService) {
        this.userService = userService;
        this.groupeService = groupeService;
        this.messageService = messageService;
        this.groupeUserService = groupeUserService;
    }

    @ApiOperation(value = "Envoie un message")
    @PostMapping("/send")
    public ResponseEntity<Object> sendMessage(@RequestBody MessageTemplate message)
    {
        try {
            User user = userService.findById(message.getUser_id());
            if(groupeUserService.CountByGroupeIdAndUserId(message.getGroup_id(), message.getUser_id())) {
                if (!clientPool.containsKey(message.getGroup_id())) {
                    clientPool.put(message.getGroup_id(), new ClientRunnable(message.getGroup_id()));
                    clientPool.get(message.getGroup_id()).start();
                }
                clientPool.get(message.getGroup_id()).addUserToGroup(message.getUser_id());
                clientPool.get(message.getGroup_id()).sendMsg(message.toString(), userService.findById(message.getUser_id()).getUsername());

                // Sauvegarde
                Groupe groupe = groupeService.find(message.getGroup_id());
                messageService.save( new Message(message.getMessage(), user, groupe));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *
     * @param idGroupe
     * @param  idUser
     * @return
     */
    @ApiOperation(value = "Retourne tous les messages reçus par le client OCSF")
    @GetMapping("/live/{idGroupe}/{idUser}")
    public ResponseEntity<Object> getLiveMessages(@PathVariable(value = "idGroupe") Long idGroupe, @PathVariable(value="idUser") Long idUser)
    {
        try {
            if (groupeUserService.CountByGroupeIdAndUserId(idGroupe, idUser)) {
                if (!clientPool.containsKey(idGroupe)) {
                    clientPool.put(idGroupe, new ClientRunnable(idGroupe));
                    clientPool.get(idGroupe).start();
                }

                ClientRunnable clientRunnable = clientPool.get(idGroupe);
                clientRunnable.addUserToGroup(idUser);
                String messagesEnAttente = clientRunnable.getMessagesEnAttente(idUser);
                clientRunnable.viderBuffer(idUser);    // /!\ NE SERT ACTUELLEMENT PAS
                return new ResponseEntity<Object>(messagesEnAttente, HttpStatus.OK);
            }
        }
        catch (AutorisationException e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<Object>("{}", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ApiOperation(value = "Retourne tous les messages enregistrés")
    @GetMapping("/saved/{idGroupe}/{idUser}")
    public ResponseEntity<Object> getSavedMessages(@PathVariable(value = "idGroupe") Long idGroupe, @PathVariable(value="idUser") Long idUser)
    {
        try {
            if (groupeUserService.CountByGroupeIdAndUserId(idGroupe, idUser)) {
                String messagesEnAttente = messageService.get(idGroupe);
                return new ResponseEntity<Object>(messagesEnAttente, HttpStatus.OK);
            }
        }
        catch (AutorisationException e) {
            return new ResponseEntity<Object>("{}", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ApiOperation(value = "Ajoute un utilisateur au client OCSF du groupe")
    @PostMapping("/add/{idGroupe}/{idUser}")
    public ResponseEntity<Object> addUserToOCSFClient(@PathVariable(value = "idGroupe") Long idGroupe, @PathVariable(value="idUser") Long idUser)
    {
        try {
            if (groupeUserService.CountByGroupeIdAndUserId(idGroupe, idUser)) {
                if (!clientPool.containsKey(idGroupe)) {
                    clientPool.put(idGroupe, new ClientRunnable(idGroupe));
                    clientPool.get(idGroupe).start();
                }

                ClientRunnable clientRunnable = clientPool.get(idGroupe);
                clientRunnable.addUserToGroup(idUser);
                return new ResponseEntity<Object>(HttpStatus.OK);
            }
        }
        catch (AutorisationException e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<Object>("{}", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}