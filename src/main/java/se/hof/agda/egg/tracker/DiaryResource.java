package se.hof.agda.egg.tracker;

import se.hof.agda.egg.tracker.dto.DiaryEntryDTO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("diary")
public class DiaryResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/entry")
    public String createDiaryEntry(DiaryEntryDTO body) {
        return "nr of eggs posted " + body.getEggs();
    }

}