package club.mrdaisite.torder.torderadmin.service.impl;

import club.mrdaisite.torder.torderadmin.component.CustomException;
import club.mrdaisite.torder.torderadmin.dto.AdminInsertParamDTO;
import club.mrdaisite.torder.torderadmin.dto.AdminResultDTO;
import club.mrdaisite.torder.torderadmin.dto.AdminUpdateParamDTO;
import club.mrdaisite.torder.torderadmin.service.AdminAdminService;
import club.mrdaisite.torder.torderadmin.service.AdminRoleService;
import club.mrdaisite.torder.torderadmin.util.FuncUtils;
import club.mrdaisite.torder.tordermbg.mapper.AdminMapper;
import club.mrdaisite.torder.tordermbg.mapper.AdminRoleRelationMapper;
import club.mrdaisite.torder.tordermbg.mapper.RoleMapper;
import club.mrdaisite.torder.tordermbg.model.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author dai
 * @date 2019/03/21
 */
@Service
public class AdminAdminServiceImpl implements AdminAdminService {
    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private AdminRoleService adminRoleService;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private AdminRoleRelationMapper adminRoleRelationMapper;

    @Override
    public List<Object> listAdmin(Integer page, Integer perPage, String sortBy, String order) {
        PageHelper.startPage(page, perPage, sortBy + " " + order);
        List<Admin> adminList = adminMapper.selectByExample(new AdminExample());
        PageInfo pageInfo = new PageInfo<>(adminList);
        List<Object> pageInfoList = pageInfo.getList();
        List<Object> targetList = new ArrayList<>();
        for (int i = 0; i < pageInfoList.size(); i++) {
            targetList.add(new AdminResultDTO());
        }
        return new FuncUtils().beanUtilsCopyListProperties(pageInfoList, targetList);
    }

    @Override
    public List<Admin> listAdminByRoleId(Long roleId) throws CustomException {
        if (!adminRoleService.roleExists(roleId)) {
            throw new CustomException("不存在的角色组");
        }
        AdminRoleRelationExample adminRoleRelationExample = new AdminRoleRelationExample();
        adminRoleRelationExample.or().andRoleIdEqualTo(roleId);
        List<AdminRoleRelation> adminRoleRelationList = adminRoleRelationMapper.selectByExample(adminRoleRelationExample);
        return adminRoleRelationList.stream()
                .map(adminRoleRelation -> adminMapper.selectByPrimaryKey(adminRoleRelation.getAdminId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public AdminResultDTO getAdminById(Long id) throws CustomException {
        AdminResultDTO adminResultDTO = new AdminResultDTO();
        Admin admin = adminMapper.selectByPrimaryKey(id);
        if (admin == null) {
            throw new CustomException("不存在的用户");
        }
        BeanUtils.copyProperties(admin, adminResultDTO);
        return adminResultDTO;
    }

    @Override
    public Admin getAdminByUsername(String username) {
        AdminExample adminExample = new AdminExample();
        adminExample.or().andUsernameEqualTo(username);
        List<Admin> adminList = adminMapper.selectByExample(adminExample);
        if (adminList != null && adminList.size() > 0) {
            return adminList.get(0);
        }
        return null;
    }

    @Override
    public AdminResultDTO insertAdmin(AdminInsertParamDTO adminInsertParamDTO, String roleName) {
        Admin admin = new Admin();
        AdminRoleRelation adminRoleRelation = new AdminRoleRelation();
        AdminResultDTO adminResultDTO = new AdminResultDTO();

        RoleExample roleExample = new RoleExample();
        roleExample.or().andNameEqualTo(roleName);
        List<Role> roleList = roleMapper.selectByExample(roleExample);
        Role role = roleList.get(0);
        BeanUtils.copyProperties(adminInsertParamDTO, admin);

        String bCryptPassword = bCryptPasswordEncoder.encode(adminInsertParamDTO.getPassword());
        admin.setPassword(bCryptPassword);
        admin.setEnabled(adminInsertParamDTO.getEnabled());
        admin.setGmtCreate(new Date());
        admin.setGmtModified(new Date());
        adminMapper.insert(admin);
        adminRoleRelation.setAdminId(admin.getId());
        adminRoleRelation.setRoleId(role.getId());
        adminRoleRelation.setGmtCreate(new Date());
        adminRoleRelation.setGmtModified(new Date());
        adminRoleRelationMapper.insert(adminRoleRelation);
        BeanUtils.copyProperties(admin, adminResultDTO);
        return adminResultDTO;
    }

    @Override
    public AdminResultDTO updateAdmin(Long id, AdminUpdateParamDTO adminUpdateParamDTO) throws AccessDeniedException, CustomException {
        Admin admin = adminMapper.selectByPrimaryKey(id);
        BeanUtils.copyProperties(adminUpdateParamDTO, admin);
        if (admin.getPassword() != null) {
            admin.setPassword(bCryptPasswordEncoder.encode(admin.getPassword()));
        }
        admin.setGmtModified(new Date());
        adminMapper.updateByPrimaryKeySelective(admin);
        return getAdminById(id);
    }

    @Override
    public void deleteAdmin(Long id) {
        Admin admin = adminMapper.selectByPrimaryKey(id);
        AdminRoleRelationExample adminRoleRelationExample = new AdminRoleRelationExample();
        adminRoleRelationExample.or().andAdminIdEqualTo(admin.getId());
        adminRoleRelationMapper.deleteByExample(adminRoleRelationExample);
        adminMapper.deleteByPrimaryKey(id);
    }

    @Override
    public Boolean adminExists(Long id) {
        return adminMapper.selectByPrimaryKey(id) != null;
    }
}