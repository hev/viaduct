package com.example.starwars.modules.filmography.characters.models

import jakarta.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

/**
 * Repository object for managing character data in the Star Wars demo application.
 *
 */
@Singleton
class CharacterRepository {
    /** Atomic integer to generate unique character IDs. */
    private val charactersIdSequence = AtomicInteger(1)

    /** In-memory list to store character entities. */
    private val characters = mutableListOf(
        Character(
            id = "${charactersIdSequence.andIncrement}",
            name = "Luke Skywalker",
            birthYear = "19BBY",
            eyeColor = "blue",
            gender = "male",
            hairColor = "blond",
            height = 172,
            mass = 77f,
            homeworldId = "1",
            speciesId = "1"
        ),
        Character(
            id = "${charactersIdSequence.andIncrement}",
            name = "Princess Leia",
            birthYear = "19BBY",
            eyeColor = "brown",
            gender = "female",
            hairColor = "brown",
            height = 150,
            mass = 49f,
            homeworldId = "2",
            speciesId = "1"
        ),
        Character(
            id = "${charactersIdSequence.andIncrement}",
            name = "Han Solo",
            birthYear = "29BBY",
            eyeColor = "brown",
            gender = "male",
            hairColor = "brown",
            height = 180,
            mass = 80f,
            homeworldId = "3",
            speciesId = "1"
        ),
        Character(
            id = "${charactersIdSequence.andIncrement}",
            name = "Darth Vader",
            birthYear = "41.9BBY",
            eyeColor = "yellow",
            gender = "male",
            hairColor = "none",
            height = 202,
            mass = 136f,
            homeworldId = "1",
            speciesId = "1"
        ),
        Character(
            id = "${charactersIdSequence.andIncrement}",
            name = "Obi-Wan Kenobi",
            birthYear = "57BBY",
            eyeColor = "blue-gray",
            gender = "male",
            hairColor = "auburn, white",
            height = 182,
            mass = 77f,
            homeworldId = "4",
            speciesId = "1"
        )
    )

    init {
        val seedCount = System.getenv("SEED_CHARACTERS")?.toIntOrNull() ?: 0
        if (seedCount > 0) {
            bulkSeed(seedCount)
            println("Seeded $seedCount synthetic characters (total: ${characters.size})")
        }
    }

    /**
     *  Retrieves all characters in the repository.
     *
     * @return A list of all character entities.
     */
    fun findAll(): List<Character> = characters

    /**
     * Finds characters by a list of IDs and returns them as a map.
     *
     * @param characterIds The list of character IDs to search for.
     * @return A map where the key is the character ID and the value is the character entity.
     */
    fun findCharactersAsMap(characterIds: List<String>): Map<String, Character> {
        return characterIds.mapNotNull { findById(it) }.associateBy { it.id }
    }

    /**
     * Finds a character by its unique ID.
     *
     * @param id The ID of the character to find.
     * @return The character entity if found, null otherwise.
     */
    fun findById(id: String): Character? = characters.find { it.id == id }

    /**
     * Finds characters whose names contain the specified substring (case-insensitive).
     *
     * @param name The substring to search for in character names.
     * @return A list of characters whose names match the search criteria.
     */
    fun findCharactersByName(name: String): List<Character> {
        return characters.filter { it.name.contains(name, ignoreCase = true) }
    }

    /**
     * Finds characters by a list of IDs.
     *
     * @param ids The list of character IDs to search for.
     * @return A list of characters whose IDs are in the provided list.
     */
    fun findCharactersByIdList(ids: List<String>): List<Character> {
        return characters.filter { ids.contains(it.id) }
    }

    /**
     * Finds characters by their year of birth.
     *
     * @param birthYear The birth year to search for (e.g., "19BBY").
     * @return A list of characters born in the specified year.
     */
    fun findCharactersByYearOfBirth(birthYear: String): List<Character> {
        return characters.filter { it.birthYear == birthYear }
    }

    /**
     * Adds a new character to the repository.
     *
     * @param character The character entity to add (ID will be auto-generated).
     * @return The newly added character with its assigned ID.
     */
    fun add(character: Character): Character {
        val newCharacter = character.copy(id = "${charactersIdSequence.andIncrement}")
        characters.add(newCharacter)
        return newCharacter
    }

    /**
     * Bulk-inserts synthetic characters for benchmarking.
     *
     * @param count The number of synthetic characters to create.
     * @return The total number of characters after insertion.
     */
    fun bulkSeed(count: Int): Int {
        val homeworldIds = listOf("1", "2", "3", "4", "5", "6")
        val speciesIds = listOf("1", "2")
        val eyeColors = listOf("blue", "brown", "green", "hazel", "amber", "red", "yellow")
        val hairColors = listOf("blond", "brown", "black", "red", "auburn", "white", "none")

        for (i in 1..count) {
            add(
                Character(
                    id = "",
                    name = "Synth-$i",
                    birthYear = "${i % 100}BBY",
                    eyeColor = eyeColors[i % eyeColors.size],
                    gender = if (i % 2 == 0) "male" else "female",
                    hairColor = hairColors[i % hairColors.size],
                    height = 150 + (i % 50),
                    mass = (60 + (i % 40)).toFloat(),
                    homeworldId = homeworldIds[i % homeworldIds.size],
                    speciesId = speciesIds[i % speciesIds.size]
                )
            )
        }
        return characters.size
    }

    /**
     * Deletes a character by its unique ID.
     *
     * @param id The ID of the character to delete.
     * @return True if the character was found and deleted, false otherwise.
     */
    fun delete(id: String): Boolean {
        return characters.removeIf { it.id == id }
    }

    /**
     * Updates an existing character in the repository.
     *
     * @param updatedCharacter The character entity with updated information (must include valid ID).
     * @throws IllegalArgumentException if the character with the specified ID is not found.
     */
    fun update(updatedCharacter: Character): Character {
        val index = characters.indexOfFirst { it.id == updatedCharacter.id }
        if (index != -1) {
            characters[index] = updatedCharacter
        } else {
            throw IllegalArgumentException("Character with ID ${updatedCharacter.id} not found")
        }

        return updatedCharacter
    }
}
