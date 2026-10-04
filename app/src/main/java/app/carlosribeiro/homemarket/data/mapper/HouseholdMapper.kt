package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.data.local.HouseholdEntity
import app.carlosribeiro.homemarket.data.local.MemberEntity
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole

/** Field names of `households/{householdId}`, see docs/backend.md. */
object HouseholdFields {
    const val NAME = "name"
    const val ADMIN_UID = "adminUid"
    const val INVITE_TOKEN = "inviteToken"
    const val MEMBER_UIDS = "memberUids"
    const val CREATED_AT = "createdAt"
}

object HouseholdMapper {
    fun documentToEntity(id: String, data: Map<String, Any?>): HouseholdEntity = HouseholdEntity(
        id = id,
        name = data[HouseholdFields.NAME] as? String ?: "",
        adminUid = data[HouseholdFields.ADMIN_UID] as? String ?: "",
        inviteToken = data[HouseholdFields.INVITE_TOKEN] as? String ?: "",
        memberUids = (data[HouseholdFields.MEMBER_UIDS] as? List<*>)?.filterIsInstance<String>().orEmpty()
    )

    fun entityToDomain(entity: HouseholdEntity): Household = Household(
        id = entity.id,
        name = entity.name,
        adminUid = entity.adminUid,
        inviteToken = entity.inviteToken,
        memberUids = entity.memberUids
    )

    fun memberToEntity(householdId: String, user: AppUser): MemberEntity = MemberEntity(
        householdId = householdId,
        uid = user.uid,
        displayName = user.displayName,
        email = user.email,
        photoUrl = user.photoUrl,
        role = if (user.role == UserRole.ADMIN) UserFields.ROLE_ADMIN else UserFields.ROLE_MEMBER
    )

    fun memberToDomain(entity: MemberEntity): AppUser = AppUser(
        uid = entity.uid,
        displayName = entity.displayName,
        email = entity.email,
        photoUrl = entity.photoUrl,
        householdId = entity.householdId,
        role = if (entity.role == UserFields.ROLE_ADMIN) UserRole.ADMIN else UserRole.MEMBER
    )
}
