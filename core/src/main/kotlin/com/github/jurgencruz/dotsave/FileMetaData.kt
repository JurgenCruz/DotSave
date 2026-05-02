package com.github.jurgencruz.dotsave

import kotlinx.serialization.Serializable

@Serializable
data class FileMetaData(val owner: String, val permissions: String)