package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_LOCATION;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PERSON;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TIME;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.logic.commands.exceptions.CommandException;

import seedu.address.model.Meeting;

import seedu.address.model.ReadOnlyMeeting;

import seedu.address.model.asana.PostTask;

import seedu.address.model.exceptions.DuplicateMeetingException;

import seedu.address.model.exceptions.IllegalIdException;

import seedu.address.model.person.InternalId;
import seedu.address.storage.asana.storage.AsanaCredentials;

//@@author Sri-vatsa
/**
 * Adds a new meeting to the address book.
 */
public class AddMeetingCommand extends UndoableCommand {

    public static final String COMMAND_WORD = "addMeeting";
    public static final String COMMAND_ALIAS = "am";

    public static final String GOOGLE_ADDRESS = "www.google.com";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a meeting to the address book. "
            + "Parameters: "
            + PREFIX_DATE + "DATE "
            + PREFIX_TIME + "TIME "
            + PREFIX_LOCATION + "LOCATION "
            + PREFIX_NOTES + "NOTES "
            + PREFIX_PERSON + "PERSON 1 "
            + PREFIX_PERSON + "PERSON 2 ...\n"
            + "Example: "
            + COMMAND_WORD + " "
            + PREFIX_DATE + "20/11/2017 "
            + PREFIX_TIME + "1800 "
            + PREFIX_LOCATION + "UTown Starbucks "
            + PREFIX_NOTES + "Project Meeting "
            + PREFIX_PERSON + "1";


    public static final String MESSAGE_SUCCESS_BOTH = "New meeting added locally and to Asana!";
    public static final String MESSAGE_SUCCESS_NO_INET = "New meeting added locally!\n "
            + "Connect to the internet to post the meeting on Asana.";
    public static final String MESSAGE_SUCCESS_ASANA_NO_CONFIG = "New meeting added locally!\n"
            + "Setup Asana to post the meeting on Asana.";
    public static final String MESSAGE_SUCCESS_LOCAL = "New meeting added locally!\n"
            + "Connect to the internet and setup Asana to post a meeting on Asana.";
    public static final String MESSAGE_ERROR_INVALID_INDEX = "Person with index %1$d not found.\n";
    public static final String MESSAGE_DUPLICATE_MEETING = "This meeting already exists in the address book";
    public static final String MESSAGE_INVALID_ID = "Please input a valid person id!";
    public static final String MESSAGE_TEMPLATE = COMMAND_WORD + " "
            + PREFIX_DATE + "DATE "
            + PREFIX_TIME + "TIME "
            + PREFIX_LOCATION + "LOCATION "
            + PREFIX_NOTES + "NOTES "
            + PREFIX_PERSON + "PERSON ...";


    private Meeting toAdd;
    private LocalDateTime localDateTime;
    private String location;
    private String notes;
    private ArrayList<Index> idList;

    public AddMeetingCommand(ReadOnlyMeeting meeting) {
        toAdd = new Meeting(meeting);
    }

    public AddMeetingCommand(LocalDateTime localDateTime, String location, String notes, ArrayList<Index> idList) {
        this.localDateTime = localDateTime;
        this.location = location;
        this.notes = notes;
        this.idList = idList;
    }

    @Override
    public CommandResult executeUndoableCommand() throws CommandException {
        requireNonNull(model);

        ArrayList<InternalId> internalIds = null;
        try {
            internalIds = convertVisibleIdsToInternal(this.idList);
        } catch (IllegalValueException e) {
            throw new CommandException(String.format(e.getMessage()));
        }

        toAdd = new Meeting(localDateTime, location, notes, internalIds);

        AsanaCredentials asanaCredentials = new AsanaCredentials();

        //if there is internet connection && asana is configured
        if (isThereInternetConnection() && asanaCredentials.getIsAsanaConfigured()) {

            try {

                //add meeting on Asana
                PostTask newAsanaTask = null;
                try {
                    newAsanaTask = new PostTask(toAdd.getNotes(), toAdd.getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                newAsanaTask.execute();

                //add meeting locally
                model.addMeeting(toAdd);

            } catch (DuplicateMeetingException e) {
                throw new CommandException(MESSAGE_DUPLICATE_MEETING);
            } catch (IllegalIdException ive) {
                throw new CommandException(MESSAGE_INVALID_ID);
            }

            return new CommandResult(String.format(MESSAGE_SUCCESS_BOTH, toAdd));
        } else {
            //only add meeting locally, not on Asana
            try {
                model.addMeeting(toAdd);
            } catch (DuplicateMeetingException e) {
                throw new CommandException(MESSAGE_DUPLICATE_MEETING);
            } catch (IllegalIdException ive) {
                throw new CommandException(MESSAGE_INVALID_ID);
            }

            //there is a stable internet connection but Asana is not configured
            if (isThereInternetConnection() && !asanaCredentials.getIsAsanaConfigured()) {
                return new CommandResult(MESSAGE_SUCCESS_ASANA_NO_CONFIG);
            } else if (!isThereInternetConnection() && asanaCredentials.getIsAsanaConfigured()) {
                //No internet connection but Asana is configured
                return new CommandResult(MESSAGE_SUCCESS_NO_INET);
            } else {
                //There is no internet connection and Asana is not configured
                return new CommandResult(MESSAGE_SUCCESS_LOCAL);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof AddMeetingCommand // instanceof handles nulls
                && Objects.equals(this.location, ((AddMeetingCommand) other).location))
                && this.notes.equals(((AddMeetingCommand) other).notes)
                && this.localDateTime.equals(((AddMeetingCommand) other).localDateTime)
                && this.idList.equals(((AddMeetingCommand) other).idList);
    }

    /**
     * Check if there is an internet connection available
     * @return isThereInternetCOnnection, true if there is a connection and false otherwise
     */
    private boolean isThereInternetConnection() {
        Socket sock = new Socket();
        InetSocketAddress addr = new InetSocketAddress(GOOGLE_ADDRESS, 80);
        try {
            sock.connect(addr, 300);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                return false;
            }
        }
    }

    //@@author liuhang0213

    /**
     * Converts a list of visible indexes used in meeting to internal indexes
     *
     * @param visibleIds
     * @return list of internal ids
     * @throws IllegalValueException when the given visible index is not in the addressbook
     */
    private ArrayList<InternalId> convertVisibleIdsToInternal(ArrayList<Index> visibleIds)
            throws IllegalValueException {
        ArrayList<InternalId> internalIds = new ArrayList<>();
        for (Index visibleId : visibleIds) {
            try {
                internalIds.add(model.visibleToInternalId(visibleId));
            } catch (IllegalValueException e) {
                throw new IllegalValueException(String.format(MESSAGE_ERROR_INVALID_INDEX, visibleId.getOneBased()));
            }
        }
        return internalIds;
    }
}
