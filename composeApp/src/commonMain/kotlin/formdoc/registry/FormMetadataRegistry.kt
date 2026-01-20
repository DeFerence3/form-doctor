package formdoc.registry

import me.deference.formdoc.FormMetadata

expect object FormMetadataRegistry{
    inline fun <reified T : Any> get(): FormMetadata<T>?
}