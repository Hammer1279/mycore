/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.metadata.history;

import java.io.Serializable;
import java.time.Instant;

import org.mycore.backend.jpa.MCRObjectIDConverter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entity implementation class for Entity: MCRMetaHistoryItem
 *
 */
@Entity
@Table(name = "MCRMetaHistory",
    indexes = {
        @Index(name = "IDX_ID_TIME", columnList = "id,time"),
        @Index(name = "IDX_TIME", columnList = "time")
    })
@NamedQueries({
    @NamedQuery(name = "MCRMetaHistory.getLastOfType",
        query = "SELECT MAX(time) FROM MCRMetaHistoryItem i WHERE i.id=:id and i.eventType=:type"),
    @NamedQuery(name = "MCRMetaHistory.getLastEventByID",
        query = "SELECT a FROM MCRMetaHistoryItem a "
            + "WHERE a.time in (SELECT max(time) as time FROM MCRMetaHistoryItem b "
            + "WHERE a.id=b.id AND time BETWEEN :from AND :until) "
            + "AND a.eventType=:eventType"),
    @NamedQuery(name = "MCRMetaHistory.getFirstDate", query = "SELECT MIN(time) from MCRMetaHistoryItem"),
    @NamedQuery(name = "MCRMetaHistory.getHighestID",
        query = "SELECT MAX(id) from MCRMetaHistoryItem WHERE ID like :looksLike"),
    @NamedQuery(name = "MCRMetaHistory.getNextActiveIDs",
        query = "SELECT c"
            + " FROM MCRMetaHistoryItem c"
            + " WHERE (:afterID is null or c.id >:afterID)"
            + "   AND c.eventType='c'"
            + "   AND (:kind!='object' OR c.id NOT LIKE '%\\_derivate\\_%')"
            + "   AND (:kind!='derivate' OR c.id LIKE '%\\_derivate\\_%')"
            + "   AND (NOT EXISTS (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventType='d' AND c.id=d.id)"
            + "        OR c.time > ALL (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventType='d' AND c.id=d.id))"
            + " ORDER by c.id"),
    @NamedQuery(name = "MCRMetaHistory.countActiveIDs",
        query = "SELECT count(c)"
            + " FROM MCRMetaHistoryItem c"
            + " WHERE c.eventType='c'"
            + "   AND (:kind!='object' OR c.id NOT LIKE '%\\_derivate\\_%')"
            + "   AND (:kind!='derivate' OR c.id LIKE '%\\_derivate\\_%')"
            + "   AND (NOT EXISTS (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventType='d' AND c.id=d.id)"
            + "        OR c.time > ALL (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventType='d' AND c.id=d.id))")
})
public class MCRMetaHistoryItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long internalid;

    @Column(length = MCRObjectID.MAX_LENGTH)
    @Convert(converter = MCRObjectIDConverter.class)
    @Basic
    private MCRObjectID id;

    private Instant time;

    @Convert(converter = MCRMetadataHistoryEventTypeConverter.class)
    @Column(length = 1)
    private MCRMetadataHistoryEventType eventType;

    private String userID;

    private String userIP;

    private static final long serialVersionUID = 1L;

    static MCRMetaHistoryItem createdNow(MCRObjectID id) {
        return now(id, MCRMetadataHistoryEventType.Create);
    }

    static MCRMetaHistoryItem deletedNow(MCRObjectID id) {
        return now(id, MCRMetadataHistoryEventType.Delete);
    }

    static MCRMetaHistoryItem now(MCRObjectID id, MCRMetadataHistoryEventType type) {
        MCRMetaHistoryItem historyItem = new MCRMetaHistoryItem();
        historyItem.setId(id);
        historyItem.setTime(Instant.now());
        historyItem.setEventType(type);
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            historyItem.setUserID(currentSession.getUserInformation().getUserID());
            historyItem.setUserIP(currentSession.getCurrentIP());
        }
        return historyItem;
    }

    public long getInternalid() {
        return this.internalid;
    }

    public void setInternalid(long internalid) {
        this.internalid = internalid;
    }

    public MCRObjectID getId() {
        return this.id;
    }

    public void setId(MCRObjectID id) {
        this.id = id;
    }

    public Instant getTime() {
        return this.time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public MCRMetadataHistoryEventType getEventType() {
        return this.eventType;
    }

    public void setEventType(MCRMetadataHistoryEventType eventType) {
        this.eventType = eventType;
    }

    public String getUserID() {
        return this.userID;
    }

    public void setUserID(String userId) {
        this.userID = userId;
    }

    public String getUserIP() {
        return this.userIP;
    }

    public void setUserIP(String ip) {
        this.userIP = ip;
    }

    @Override
    public String toString() {
        return "MCRMetaHistoryItem [eventType=" + eventType + ", id=" + id + ", time=" + time + ", userID=" + userID
            + ", userIP=" + userIP + "]";
    }

}
