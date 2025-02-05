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

package org.mycore.common.config;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2.SingletonKey;

/**
 * A collision resistant ConcurrentHashMap for use in {@link MCRConfiguration2}.
 * The collision resistance during update only works with {@link SingletonKey}s
 * during the {@link #computeIfAbsent(SingletonKey, Function)} operation.
 * <p> This class is not serializable despite its inherited implementation of {@link java.io.Serializable}
 * @author Tobias Lenhardt [Hammer1279]
 */
@SuppressWarnings("unchecked")
class MCRConcurrentHashMap<K extends SingletonKey, V> extends ConcurrentHashMap<K, V> {
    private HashMap<K, RemappedKey> keyMap = new HashMap<>();

    // Disable serialization
    @SuppressWarnings("unused")
    private void readObject(ObjectInputStream ois)
        throws ClassNotFoundException, IOException {
        throw new NotSerializableException();
    }

    @SuppressWarnings("unused")
    private void writeObject(ObjectOutputStream ois)
        throws IOException {
        throw new NotSerializableException();
    }

    /**
     * {@link ConcurrentHashMap#computeIfAbsent(Object, Function)} with added collision resistance
     * for {@link SingletonKeys} to allow changes to the map during computation.
     * In case of collision, the key is automatically remapped via {@link RemappedKey}.
     * It is a wrapper to modify the hashcode of the given SingletonKey to assign a different bucket
     * within the internal table, in an attempt to resolve the collision.
     * 
     * <p>The mapping function must not modify the map during computation of any key other then a {@link SingletonKey}
     * @see java.util.concurrent.ConcurrentHashMap#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (keyMap.containsKey(key)) {
            return super.computeIfAbsent((K) keyMap.get(key), mappingFunction);
        } else {
            try {
                return super.computeIfAbsent(key, mappingFunction);
            } catch (IllegalStateException e) { // recursive update fix
                if (key instanceof SingletonKey && "Recursive update".equals(e.getMessage())) {
                    LogManager.getLogger().warn("collision detected, remapping key...");
                    RemappedKey newKey = new RemappedKey(key);
                    keyMap.put(key, newKey);
                    return tryCompute(key, newKey, mappingFunction);
                }
                throw e;
            }
        }
    }

    private V tryCompute(K key, RemappedKey newKey, Function<? super K, ? extends V> mappingFunction) {
        try {
            return super.computeIfAbsent((K) newKey, mappingFunction);
        } catch (IllegalStateException err) {
            LogManager.getLogger().warn("collision while remapping, regenerating seed...");
            keyMap.put(key, newKey.reGenSeed());
            return tryCompute(key, newKey, mappingFunction);
        } catch (Exception exception) {
            throw exception;
        }
    }

    /**
     * Wrapper for {@link SingletonKey} to modify the HashCode value.
     */
    private class RemappedKey extends SingletonKey {
        private K key;
        private int seed;

        RemappedKey(K key) {
            super(null, null);
            this.key = key;
            this.seed = (int) (Math.random() * Integer.SIZE + 1);
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ seed;
        }

        /**
         * Generates a new Seed to get a different HashCode
         * @return {@code this} for chaining
         */
        public RemappedKey reGenSeed() {
            this.seed = (int) (Math.random() * Integer.SIZE + 1) ^ (seed * 0x15);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (this == obj) {
                return true;
            } else if (this.getClass() != obj.getClass()) {
                return false;
            } else {
                try {
                    RemappedKey keyObj = (RemappedKey) obj;
                    return this.key.equals(keyObj.key)
                        && this.seed == keyObj.seed
                        && this.hashCode() == keyObj.hashCode();
                } catch (ClassCastException e) {
                    return false;
                }
            }
        }
    }
}
