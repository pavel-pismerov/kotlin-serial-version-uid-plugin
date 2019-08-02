package com.ppismerov.ksvu.hash

import com.intellij.psi.impl.cache.ModifierFlags.ABSTRACT_MASK
import com.intellij.psi.impl.cache.ModifierFlags.OPEN_MASK
import com.intellij.psi.impl.cache.ModifierFlags.PROTECTED_MASK
import com.intellij.psi.impl.cache.ModifierFlags.PUBLIC_MASK
import org.apache.commons.lang3.StringUtils.SPACE
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import kotlin.experimental.and
import kotlin.math.min

private const val DEFAULT_HASH_VALUE = PUBLIC_MASK

fun computeHashCode(ktClass: KtClass, ktClassBody: KtClassBody) = ByteArrayOutputStream().use { baos ->
    DataOutputStream(baos).use { dos ->
        dos.addClassHash(ktClass)
        dos.addConstructorsHash(ktClass)
        dos.computeInitializersHash(ktClassBody)
        dos.addClassBodyHash(ktClassBody)
        dos.flush()
        val messageDigest = MessageDigest.getInstance("SHA")
        val byteArray = messageDigest.digest(baos.toByteArray())
        var hash = 0L
        for (index in min(byteArray.size, 8) - 1 downTo 0) {
            hash = (hash shl 8).or(byteArray[index].and(255.toByte()).toLong())
        }

        hash
    }
}

private fun DataOutputStream.addClassHash(ktClass: KtClass) {
    with(ktClass) {
        safeWriteUTF(name)
        writeInt(classModifiers())
        writeUTF(superTypeListEntries.sortedBy { it.text }.joinToString(SPACE))
    }
}

private fun KtClass.classModifiers(): Int {
    var hash = DEFAULT_HASH_VALUE
    if (isPublic) hash += PUBLIC_MASK
    if (isProtected()) hash += PROTECTED_MASK
    if (isAbstract()) hash += ABSTRACT_MASK
    if (isInheritable()) hash += OPEN_MASK
    return hash
}

private fun DataOutputStream.addConstructorsHash(ktClass: KtClass) {
    with(ktClass) {
        primaryConstructor?.let { calculateConstructorHash(it) }
        calculateConstructorsHash(secondaryConstructors)
    }
}

private fun  <T : KtConstructor<T>> DataOutputStream.calculateConstructorsHash(constructors: List<KtConstructor<T>>) {
    constructors.forEach {
        calculateConstructorHash(it)
    }
}

private fun <T : KtConstructor<T>> DataOutputStream.calculateConstructorHash(constructor: KtConstructor<T>) {
    with(constructor) {
        var hash = DEFAULT_HASH_VALUE
        if (isPublic) hash += PUBLIC_MASK
        if (isProtected()) hash += PROTECTED_MASK
        if (isAbstract()) hash += ABSTRACT_MASK
        if (isOverridable()) hash += OPEN_MASK

        safeWriteUTF(name)
        writeInt(hash)
        safeWriteUTF(text)
    }
}

private fun DataOutputStream.addClassBodyHash(ktClassBody: KtClassBody) {
    with(ktClassBody) {
        computeDeclarationsHash(children.filterIsInstance<KtProperty>())
        computeDeclarationsHash(children.filterIsInstance<KtNamedFunction>())
    }
}

private fun DataOutputStream.computeInitializersHash(ktClassBody: KtClassBody) {
    ktClassBody.anonymousInitializers.forEach {
        safeWriteUTF(it.name)
        writeInt(8)
        safeWriteUTF(it.text)
    }
}

private fun DataOutputStream.computeDeclarationsHash(declarations: List<KtDeclaration>) = declarations.forEach {
    computeDeclarationHash(it)
}

private fun DataOutputStream.computeDeclarationHash(it: KtDeclaration) {
    if (!it.isPrivate()) {
        var hash = DEFAULT_HASH_VALUE
        if (it.isPublic) hash += PUBLIC_MASK
        if (it.isProtected()) hash += PROTECTED_MASK
        if (it.isOverridable()) hash += OPEN_MASK

        safeWriteUTF(it.name)
        writeInt(hash)
        safeWriteUTF(it.modifierList?.text)
    }
}

private fun DataOutputStream.safeWriteUTF(text: String?) = text?.let { writeUTF(it) }